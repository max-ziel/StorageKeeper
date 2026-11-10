package io.github.maxziel.storagekeeper.application.port;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a result list plus the information needed to show page navigation.
 *
 * @param page 0-based number of this page
 * @param size requested page size; {@code content} can be shorter on the last page
 * @param totalElements number of matching entries over all pages
 */
public record ResultPage<T>(List<T> content, int page, int size, long totalElements) {

    public ResultPage {
        content = List.copyOf(content);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }

    /** Converts the entries, e.g. from domain objects to JSON responses; the paging data stays the same. */
    public <R> ResultPage<R> map(Function<T, R> mapper) {
        return new ResultPage<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}
