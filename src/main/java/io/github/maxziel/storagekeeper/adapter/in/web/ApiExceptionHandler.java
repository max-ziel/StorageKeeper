package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.AlreadyInHouseholdException;
import io.github.maxziel.storagekeeper.application.NoHouseholdException;
import io.github.maxziel.storagekeeper.application.PantryItemNotFoundException;
import io.github.maxziel.storagekeeper.application.ShoppingListEntryNotFoundException;
import io.github.maxziel.storagekeeper.application.StorageLocationAlreadyExistsException;
import io.github.maxziel.storagekeeper.application.StorageLocationNotFoundException;
import io.github.maxziel.storagekeeper.application.UnknownInviteCodeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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

    @ExceptionHandler(ShoppingListEntryNotFoundException.class)
    ProblemDetail handleShoppingListEntryNotFound(ShoppingListEntryNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /** Logged in, but without a household there is no data the user may see or change. */
    @ExceptionHandler(NoHouseholdException.class)
    ProblemDetail handleNoHousehold(NoHouseholdException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(AlreadyInHouseholdException.class)
    ProblemDetail handleAlreadyInHousehold(AlreadyInHouseholdException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    /** The code comes from the request body, so the request itself is invalid. */
    @ExceptionHandler(UnknownInviteCodeException.class)
    ProblemDetail handleUnknownInviteCode(UnknownInviteCodeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /**
     * Missing or invalid bearer token. Not thrown by controllers but passed in by the entry point
     * in {@link SecurityConfiguration}, which has already set the {@code WWW-Authenticate} header.
     */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleUnauthenticated(AuthenticationException exception) {
        String detail = exception instanceof OAuth2AuthenticationException oauth2Exception
                && oauth2Exception.getError().getDescription() != null
                ? oauth2Exception.getError().getDescription()
                : "A valid bearer token is required.";
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    }
}