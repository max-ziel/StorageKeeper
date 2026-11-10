package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.*;
import io.github.maxziel.storagekeeper.domain.DomainValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;

/**
 * Translates application and domain exceptions into RFC 9457 problem responses.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler}, which turns Spring MVC's own exceptions (invalid JSON, failed
 * Bean Validation, unknown path, wrong method …) into problem responses with the matching status. Without this,
 * the catch-all {@link #handleUnexpected} would answer them with {@code 500}.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CONFLICT_DETAIL = "The request conflicts with data that was changed at the same time. "
        + "Please reload and try again.";
    static final String UNEXPECTED_DETAIL = "An unexpected error occurred.";

    /**
     * SQL state of a unique or primary key violation (SQL standard, used by PostgreSQL).
     */
    private static final String UNIQUE_VIOLATION = "23505";

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * Spring reports a unique violation as {@link DuplicateKeyException} only if it translated the SQL error itself;
     * through Hibernate it is a plain {@link DataIntegrityViolationException}, so the SQL state is checked as well.
     */
    private static boolean isUniqueViolation(DataIntegrityViolationException exception) {
        if (exception instanceof DuplicateKeyException) {
            return true;
        }
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && UNIQUE_VIOLATION.equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    @ExceptionHandler(PantryItemNotFoundException.class)
    ProblemDetail handleNotFound(PantryItemNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /**
     * The unknown location comes from the request body, so the request itself is invalid.
     */
    @ExceptionHandler(StorageLocationNotFoundException.class)
    ProblemDetail handleUnknownStorageLocation(StorageLocationNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(StorageLocationAlreadyExistsException.class)
    ProblemDetail handleDuplicateStorageLocation(StorageLocationAlreadyExistsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    /**
     * A rule of a value object or entity is broken; the message is written for the client.
     */
    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail handleInvalidValue(DomainValidationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ShoppingListEntryNotFoundException.class)
    ProblemDetail handleShoppingListEntryNotFound(ShoppingListEntryNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /**
     * Logged in, but without a household there is no data the user may see or change.
     */
    @ExceptionHandler(NoHouseholdException.class)
    ProblemDetail handleNoHousehold(NoHouseholdException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(AlreadyInHouseholdException.class)
    ProblemDetail handleAlreadyInHousehold(AlreadyInHouseholdException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    /**
     * The code comes from the request body, so the request itself is invalid.
     */
    @ExceptionHandler(UnknownInviteCodeException.class)
    ProblemDetail handleUnknownInviteCode(UnknownInviteCodeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /**
     * A unique constraint of the database rejected the change. The services check uniqueness before writing, so this
     * only happens when two requests pass the check at the same time, e.g. two locations with the same name; that is
     * a conflict ({@code 409}). Any other constraint (check, not null, foreign key) guards a rule the code should
     * already have kept, so its violation is a bug ({@code 500}). The database message names tables and columns,
     * so it is logged, not returned.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleConstraintViolation(DataIntegrityViolationException exception) {
        if (!isUniqueViolation(exception)) {
            return handleUnexpected(exception);
        }
        log.warn("Rejected by a unique constraint: {}", exception.getMostSpecificCause().getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, CONFLICT_DETAIL);
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

    /**
     * Everything else is a bug, e.g. an {@link IllegalStateException}. The client gets a {@code ProblemDetail}
     * like for every other error, but without the message, which may contain internal details.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_DETAIL);
    }
}
