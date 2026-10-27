package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps pantry items in memory. Data is lost on restart; replaced by a
 * database adapter in M2.
 */
@Repository
public class InMemoryPantryItemRepository implements PantryItemRepository {

    private final Map<PantryItemId, PantryItem> items = new ConcurrentHashMap<>();

    @Override
    public PantryItem save(PantryItem item) {
        items.put(item.getPantryItemId(), item);
        return item;
    }

    @Override
    public Optional<PantryItem> findById(PantryItemId id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<PantryItem> findAll() {
        return List.copyOf(items.values());
    }

    @Override
    public boolean deleteById(PantryItemId id) {
        return items.remove(id) != null;
    }
}
