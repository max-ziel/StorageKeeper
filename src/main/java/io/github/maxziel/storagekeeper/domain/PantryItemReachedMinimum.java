package io.github.maxziel.storagekeeper.domain;

/**
 * The item's amount has reached its minimum stock again (or the minimum was lowered); before it was below.
 */
public record PantryItemReachedMinimum(PantryItemId pantryItemId) implements DomainEvent {
}
