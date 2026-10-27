package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.domain.PantryItemId;

/** Thrown when a use case refers to an item that does not exist. */
public class PantryItemNotFoundException extends RuntimeException {

    public PantryItemNotFoundException(PantryItemId id) {
        super("Pantry item " + id + " not found");
    }
}
