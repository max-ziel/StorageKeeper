package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.PageQuery;
import io.github.maxziel.storagekeeper.application.port.ResultPage;
import io.github.maxziel.storagekeeper.application.port.in.*;
import io.github.maxziel.storagekeeper.application.port.out.DomainEventPublisher;
import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;
import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Implements the pantry item use cases. Coordinates the domain model and the
 * repository ports; the business rules themselves live in the domain classes.
 *
 * <p>Every use case works on the current user's household only. An item of another household is
 * treated as if it did not exist (404), so the response does not reveal that the id is in use.
 */
@Service
public class PantryItemService implements
        AddPantryItemUseCase, ListPantryItemUseCase, ChangeQuantityUseCase, ChangeMinimumAmountUseCase,
        RemovePantryItemUseCase, ListExpiringPantryItemsUseCase, RestockPantryItemUseCase {

    private final PantryItemRepository repository;
    private final StorageLocationRepository storageLocationRepository;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;
    private final CurrentHousehold currentHousehold;

    public PantryItemService(
            PantryItemRepository repository,
            StorageLocationRepository storageLocationRepository,
            Clock clock,
            DomainEventPublisher eventPublisher,
            CurrentHousehold currentHousehold
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.storageLocationRepository =
                Objects.requireNonNull(storageLocationRepository, "storageLocationRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.currentHousehold = Objects.requireNonNull(currentHousehold, "currentHousehold must not be null");
    }

    /**
     * Without a location in the command the item goes to the household's default location;
     * without a minimum amount the item has no minimum stock.
     *
     * @throws StorageLocationNotFoundException if the given location does not exist in the household
     */
    @Override
    @Transactional
    public PantryItem add(AddPantryItemCommand command) {
        HouseholdId household = currentHousehold.require();
        StorageLocationId locationId = command.storageLocationId() == null
                ? defaultLocation(household)
                : requireLocationOf(household, command.storageLocationId());
        PantryItem item = PantryItem.create(
                household, command.name(), command.quantity(), command.expiryDate(), locationId);
        if (command.minimumAmount() != null) {
            item.changeMinimumAmount(command.minimumAmount());
        }
        return saveAndPublishEvents(item);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<PantryItem> search(PantryItemFilter filter, PageQuery page) {
        return repository.search(currentHousehold.require(), filter, page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpiringPantryItem> listExpiringWithin(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("days must not be negative");
        }
        HouseholdId household = currentHousehold.require();
        LocalDate today = LocalDate.now(clock);
        return repository.findWithExpiryDateUpTo(household, today.plusDays(days)).stream()
                .map(item -> new ExpiringPantryItem(item, item.daysUntilExpiry(today)))
                .toList();
    }

    @Override
    @Transactional
    public PantryItem changeQuantity(PantryItemId id, BigDecimal newAmount) {
        PantryItem item = load(id);
        item.changeQuantity(newAmount);
        return saveAndPublishEvents(item);
    }

    @Override
    @Transactional
    public PantryItem changeMinimumAmount(PantryItemId id, BigDecimal newMinimumAmount) {
        PantryItem item = load(id);
        item.changeMinimumAmount(newMinimumAmount);
        return saveAndPublishEvents(item);
    }

    @Override
    @Transactional
    public void remove(PantryItemId id) {
        load(id);
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public PantryItem restock(PantryItemId id, BigDecimal purchasedAmount) {
        PantryItem item = load(id);
        item.restock(purchasedAmount);
        return saveAndPublishEvents(item);
    }

    /** Loads an item of the current household; items of other households count as not found. */
    private PantryItem load(PantryItemId id) {
        HouseholdId household = currentHousehold.require();
        return repository.findById(id)
                .filter(item -> item.belongsTo(household))
                .orElseThrow(() -> new PantryItemNotFoundException(id));
    }

    private StorageLocationId defaultLocation(HouseholdId household) {
        return storageLocationRepository.findDefault(household)
                .orElseThrow(() -> new IllegalStateException("household " + household + " has no default location"))
                .getStorageLocationId();
    }

    private StorageLocationId requireLocationOf(HouseholdId household, StorageLocationId locationId) {
        return storageLocationRepository.findById(locationId)
                .filter(location -> location.belongsTo(household))
                .orElseThrow(() -> new StorageLocationNotFoundException(locationId))
                .getStorageLocationId();
    }

    /**
     * Saves the item first and then publishes its events. Both happen in the caller's transaction:
     * if a listener fails, the change to the item is rolled back as well.
     */
    private PantryItem saveAndPublishEvents(PantryItem item) {
        PantryItem saved = repository.save(item);
        eventPublisher.publish(item.pullDomainEvents());
        return saved;
    }
}
