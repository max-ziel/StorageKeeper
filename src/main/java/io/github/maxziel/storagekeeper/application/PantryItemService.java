package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.in.*;
import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;
import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;
import org.springframework.stereotype.Service;

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
        AddPantryItemUseCase, ListPantryItemUseCase, ChangeQuantityUseCase, RemovePantryItemUseCase,
        ListExpiringPantryItemsUseCase {

    private final PantryItemRepository repository;
    private final StorageLocationRepository storageLocationRepository;
    private final Clock clock;

    public PantryItemService(PantryItemRepository repository, StorageLocationRepository storageLocationRepository,
                             Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.storageLocationRepository =
                Objects.requireNonNull(storageLocationRepository, "storageLocationRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Without a location in the command the item goes to {@link StorageLocationId#DEFAULT}.
     *
     * @throws StorageLocationNotFoundException if the given location does not exist
     */
    @Override
    public PantryItem add(AddPantryItemCommand command) {
        StorageLocationId locationId = Objects.requireNonNullElse(command.storageLocationId(), StorageLocationId.DEFAULT);
        if (!storageLocationRepository.existsById(locationId)) {
            throw new StorageLocationNotFoundException(locationId);
        }
        PantryItem item = PantryItem.create(command.name(), command.quantity(), command.expiryDate(), locationId);
        return repository.save(item);
    }

    @Override
    public List<PantryItem> listAll() {
        return repository.findAll();
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
    public PantryItem changeQuantity(PantryItemId id, BigDecimal newAmount) {
        PantryItem item = repository.findById(id)
                .orElseThrow(() -> new PantryItemNotFoundException(id));
        item.changeQuantity(newAmount);
        return repository.save(item);
    }

    @Override
    public void remove(PantryItemId id) {
        if (!repository.deleteById(id)) {
            throw new PantryItemNotFoundException(id);
        }
    }
}
