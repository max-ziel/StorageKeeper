package io.github.maxziel.storagekeeper.application.port.out;

import io.github.maxziel.storagekeeper.application.port.PageQuery;
import io.github.maxziel.storagekeeper.application.port.ResultPage;
import io.github.maxziel.storagekeeper.application.port.in.PantryItemFilter;
import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Storage for pantry items, as needed by the application. Implemented by a
 * driven adapter, today {@code JpaPantryItemRepository} with PostgreSQL.
 *
 * <p>Lookups by id return items of any household; the caller checks {@link PantryItem#belongsTo}.
 * Queries that return many items take the household and only return its items.
 */
public interface PantryItemRepository {

    /** Inserts or replaces the item and returns it */
    PantryItem save(PantryItem pantryItem);

    Optional<PantryItem> findById(PantryItemId pantryItemId);

    /** Loads all items with the given ids; unknown ids are skipped. */
    List<PantryItem> findAllById(Collection<PantryItemId> pantryItemIds);

    /**
     * @return {@code true} if an item was removed, {@code false} if the id was unknown
     */
    boolean deleteById(PantryItemId pantryItemId);

    /**
     * Returns the household's items whose expiry date is on or before {@code latest}, including already expired
     * ones, sorted by expiry date (earliest first). Items without an expiry date are not included.
     */
    List<PantryItem> findWithExpiryDateUpTo(HouseholdId householdId, LocalDate latest);

    /**
     * Returns one page of the household's items that match the filter, sorted by name (and id for equal names).
     */
    ResultPage<PantryItem> search(HouseholdId householdId, PantryItemFilter filter, PageQuery page);
}
