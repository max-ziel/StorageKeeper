package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.PantryItemNotFoundException;
import io.github.maxziel.storagekeeper.application.StorageLocationAlreadyExistsException;
import io.github.maxziel.storagekeeper.application.StorageLocationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates application and domain exceptions into RFC 9457 problem responses. */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(PantryItemNotFoundException.class)
    ProblemDetail handleNotFound(PantryItemNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /** The unknown location comes from the request body, so the request itself is invalid. */
    @ExceptionHandler(StorageLocationNotFoundException.class)
    ProblemDetail handleUnknownStorageLocation(StorageLocationNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(StorageLocationAlreadyExistsException.class)
    ProblemDetail handleDuplicateStorageLocation(StorageLocationAlreadyExistsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidArgument(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
