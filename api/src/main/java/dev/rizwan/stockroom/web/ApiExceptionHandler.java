package dev.rizwan.stockroom.web;

import dev.rizwan.stockroom.error.InsufficientStockException;
import dev.rizwan.stockroom.error.NotFoundException;
import dev.rizwan.stockroom.error.StockContentionException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Error responses as RFC 7807 problem documents.
 *
 * <p>Every failure gets a stable {@code type} URI. Clients branch on that, not
 * on the human-readable message, so the message can be reworded without
 * breaking anyone.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final String BASE = "https://github.com/rizwan-dev/stockroom/docs/errors/";

    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail insufficientStock(InsufficientStockException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Insufficient stock");
        problem.setType(URI.create(BASE + "insufficient-stock"));
        problem.setProperty("sku", e.getSku());
        problem.setProperty("requested", e.getRequested());
        problem.setProperty("available", e.getAvailable());
        return problem;
    }

    /**
     * 503 with Retry-After semantics rather than 409: nothing is wrong with the
     * request, the server just could not serialise it against competing writes.
     * Telling the client "conflict" would suggest changing the order, which is
     * the wrong advice.
     */
    @ExceptionHandler(StockContentionException.class)
    ProblemDetail contention(StockContentionException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        problem.setTitle("Stock temporarily contended");
        problem.setType(URI.create(BASE + "stock-contention"));
        problem.setProperty("retryable", true);
        return problem;
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Not found");
        problem.setType(URI.create(BASE + "not-found"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The request body failed validation.");
        problem.setTitle("Validation failed");
        problem.setType(URI.create(BASE + "validation-failed"));
        problem.setProperty("fields", fields);
        return problem;
    }
}
