package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.in.*;
import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Implements the pantry item use cases. Coordinates the domain model and the
 * repository port; the business rules themselves live in the domain classes.
 */
@Service
public class PantryItemService implements
        AddPantryItemUseCase, ListPantryItemUseCase, ChangeQuantityUseCase, RemovePantryItemUseCase {

    private final PantryItemRepository repository;

    public PantryItemService(PantryItemRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public PantryItem add(AddPantryItemCommand command) {
        PantryItem item = PantryItem.create(command.name(), command.quantity(), command.expiryDate());
        return repository.save(item);
    }

    @Override
    public List<PantryItem> listAll() {
        return repository.findAll();
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
