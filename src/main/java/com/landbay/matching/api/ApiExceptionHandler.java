package com.landbay.matching.api;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Turns every failure into a standard RFC 9457 "problem" response ({@code application/problem+json}),
 * so callers always get the same shape of error and never a stack trace.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} gives correct problem responses for the usual
 * web errors for free (unsupported content type 415, wrong method 405, unknown path 404, and so on).
 * This class customises the two that callers hit most, and adds a safe catch-all.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	/** One invalid field in a request: which one, and what is wrong with it. */
	public record FieldProblem(String field, String message) {
	}

	/**
	 * The body parsed, but one or more fields broke their rules. Reports every bad field at once
	 * (sorted by name, so the response is predictable) in an {@code errors} list.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		List<FieldProblem> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldProblem(error.getField(), error.getDefaultMessage()))
				.sorted(Comparator.comparing(FieldProblem::field).thenComparing(FieldProblem::message))
				.toList();

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"The request is not valid. See 'errors' for the fields to correct.");
		problem.setTitle("Invalid request");
		problem.setProperty("errors", errors);

		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	/**
	 * The body could not be read at all: broken JSON, an empty body, or a value of the wrong kind.
	 * For an unknown enum value (such as a misspelt property type) the message lists the accepted values.
	 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, describe(ex));
		problem.setTitle("Malformed request");

		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	/**
	 * Anything unexpected. The caller gets a generic message (never the exception text, which could
	 * leak internals); the full detail goes to the log for whoever supports the service.
	 */
	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error while handling a request", ex);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred. Please try again or contact support.");
		problem.setTitle("Internal error");
		return problem;
	}

	// Builds the most helpful safe message for an unreadable body.
	private static String describe(HttpMessageNotReadableException ex) {
		Throwable cause = ex.getMostSpecificCause();

		// A text value that is not one of an enum's constants, e.g. "propertyType": "BUNGALOW".
		if (cause instanceof InvalidFormatException invalid && invalid.getTargetType() != null
				&& invalid.getTargetType().isEnum()) {
			String accepted = Arrays.stream(invalid.getTargetType().getEnumConstants())
					.map(constant -> ((Enum<?>) constant).name())
					.toList().toString();
			return "'" + invalid.getValue() + "' is not a valid value for '" + fieldName(invalid) + "'. Accepted values: "
					+ accepted.substring(1, accepted.length() - 1) + ".";
		}

		return "The request body is missing, is not valid JSON, or has a value of the wrong type.";
	}

	// The name of the JSON field the problem is in (the last step of the path to it).
	private static String fieldName(InvalidFormatException invalid) {
		List<tools.jackson.core.JacksonException.Reference> path = invalid.getPath();
		return path.isEmpty() ? "request" : path.get(path.size() - 1).getPropertyName();
	}

}
