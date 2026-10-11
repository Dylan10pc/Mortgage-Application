package com.landbay.matching.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds a few standard protective headers to every response. A small step: the brief does not ask for
 * authentication, so this is not a security layer, only sensible defaults for a service that returns
 * details of someone's mortgage application.
 *
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff} stops browsers guessing a different content type.</li>
 *   <li>{@code X-Frame-Options: DENY} stops the responses being shown inside another site's frame.</li>
 *   <li>{@code Referrer-Policy: no-referrer} stops the address being passed on to other sites.</li>
 *   <li>{@code Cache-Control: no-store} on the API stops caches (browsers, proxies) keeping an
 *       application's details.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityHeadersFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("Referrer-Policy", "no-referrer");

		// Only the API carries applicant details, so only it is marked not to be cached.
		if (request.getRequestURI().startsWith("/api/")) {
			response.setHeader("Cache-Control", "no-store");
		}

		chain.doFilter(request, response);
	}

}
