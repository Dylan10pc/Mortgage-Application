package com.landbay.matching.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id, so that a single request can be followed through the logs and a caller
 * can quote the id when reporting a problem.
 *
 * <p>If the caller sends an {@code X-Request-Id} header that is safe to use, it is kept (so the id can
 * be traced across systems); otherwise a fresh one is generated. The id is returned in the response
 * header and added to every log line written while the request is handled (see
 * {@code logging.pattern.level} in application.properties).
 *
 * <p>A supplied id is only trusted if it is short and made of plain characters, because it ends up in
 * log files: an id containing a line break could otherwise forge extra log lines.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	/** The header carrying the id, in both directions. */
	public static final String HEADER = "X-Request-Id";

	// The name the id is stored under for logging (matches %X{requestId} in the log pattern).
	static final String LOG_KEY = "requestId";

	// Letters, digits and . _ - only, 1 to 64 characters.
	private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		String requestId = idFor(request.getHeader(HEADER));

		// Return it to the caller and make it available to the logging for this request.
		response.setHeader(HEADER, requestId);
		MDC.put(LOG_KEY, requestId);
		try {
			chain.doFilter(request, response);
		} finally {
			// Always clear it: request threads are reused, so a stale id would leak into the next request.
			MDC.remove(LOG_KEY);
		}
	}

	// Keeps a supplied id only if it is safe; otherwise generates a new one.
	private static String idFor(String supplied) {
		if (supplied != null && SAFE_ID.matcher(supplied).matches()) {
			return supplied;
		}
		return UUID.randomUUID().toString();
	}

}
