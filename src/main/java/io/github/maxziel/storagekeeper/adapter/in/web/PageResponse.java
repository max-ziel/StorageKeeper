package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.ResultPage;

import java.util.List;

/**
 * JSON representation of one page of a list.
 *
 * @param page          0-based page number
 * @param totalElements number of matching entries over all pages
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    static <T> PageResponse<T> from(ResultPage<T> resultPage) {
        return new PageResponse<>(
            resultPage.content(),
            resultPage.page(),
            resultPage.size(),
            resultPage.totalElements(),
            resultPage.totalPages()
        );
    }
}
