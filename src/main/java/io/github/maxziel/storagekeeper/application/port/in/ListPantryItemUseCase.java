package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.application.port.PageQuery;
import io.github.maxziel.storagekeeper.application.port.ResultPage;
import io.github.maxziel.storagekeeper.domain.PantryItem;

/**
 * Lists the items in the pantry.
 */
public interface ListPantryItemUseCase {

    /**
     * Returns one page of the matching items, sorted by name.
     */
    ResultPage<PantryItem> search(PantryItemFilter filter, PageQuery page);
}
