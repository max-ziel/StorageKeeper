package io.github.maxziel.storagekeeper.application.port;

/**
 * Which page of a result list to load. Own type instead of Spring's {@code Pageable},
 * so the application layer does not depend on Spring Data.
 *
 * @param page 0-based page number
 * @param size number of entries per page, 1 to {@value #MAX_SIZE}
 */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
    }
}
