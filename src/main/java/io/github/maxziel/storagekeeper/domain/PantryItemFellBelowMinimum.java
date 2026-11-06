package io.github.maxziel.storagekeeper.domain;

/**
 * The item's amount has reached its minimum stock; before the change it was not.
 */
public record PantryItemFellBelowMinimum(PantryItemId pantryItemId) implements DomainEvent {
}
