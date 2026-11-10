package io.github.maxziel.storagekeeper.domain;

/**
 * The item's amount has fallen below its minimum stock (or the minimum was raised above it); before the change
 * it was not below.
 */
public record PantryItemFellBelowMinimum(PantryItemId pantryItemId) implements DomainEvent {
}
