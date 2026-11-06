package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.PageQuery;
import io.github.maxziel.storagekeeper.application.port.ResultPage;
import io.github.maxziel.storagekeeper.application.port.in.*;
import io.github.maxziel.storagekeeper.application.port.out.DomainEventPublisher;
import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;
import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
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
 */
@Service
public class PantryItemService implements
        AddPantryItemUseCase, ListPantryItemUseCase, ChangeQuantityUseCase, ChangeMinimumAmountUseCase,
        RemovePantryItemUseCase, ListExpiringPantryItemsUseCase {

    private final PantryItemRepository repository;
    private final StorageLocationRepository storageLocationRepository;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;

    public PantryItemService(
            PantryItemRepository repository,
            StorageLocationRepository storageLocationRepository,
            Clock clock,
            DomainEventPublisher eventPublisher
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.storageLocationRepository =
                Objects.requireNonNull(storageLocationRepository, "storageLocationRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    /**
     * Without a location in the command the item goes to {@link StorageLocationId#DEFAULT};
     * without a minimum amount the item has no minimum stock.
     *
     * @throws StorageLocationNotFoundException if the given location does not exist
     */
    @Override
    @Transactional
    public PantryItem add(AddPantryItemCommand command) {
        StorageLocationId locationId = Objects.requireNonNullElse(command.storageLocationId(), StorageLocationId.DEFAULT);
        if (!storageLocationRepository.existsById(locationId)) {
            throw new StorageLocationNotFoundException(locationId);
        }
        PantryItem item = PantryItem.create(command.name(), command.quantity(), command.expiryDate(), locationId);
        if (command.minimumAmount() != null) {
            item.changeMinimumAmount(command.minimumAmount());
        }
        return saveAndPublishEvents(item);
    }

    @Override
    public ResultPage<PantryItem> search(PantryItemFilter filter, PageQuery page) {
        return repository.search(filter, page);
    }

    @Override
    public List<ExpiringPantryItem> listExpiringWithin(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("days must not be negative");
        }
        LocalDate today = LocalDate.now(clock);
        return repository.findWithExpiryDateUpTo(today.plusDays(days)).stream()
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
    public void remove(PantryItemId id) {
        if (!repository.deleteById(id)) {
            throw new PantryItemNotFoundException(id);
        }
    }

    private PantryItem load(PantryItemId id) {
        return repository.findById(id).orElseThrow(() -> new PantryItemNotFoundException(id));
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
