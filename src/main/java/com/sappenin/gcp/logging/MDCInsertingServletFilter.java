package com.sappenin.gcp.logging;

import ch.qos.logback.classic.ClassicConstants;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static ch.qos.logback.classic.ClassicConstants.REQUEST_USER_AGENT_MDC_KEY;
import static com.sappenin.gcp.logging.Constants.*;

/**
 * A {@link Filter} that inserts HTTP request information into the MDC so that it can be logged properly on each
 * request.
 */
public class MDCInsertingServletFilter implements Filter {

    // Added to each request header to uniquely identify it during downstream log processing.
    static final String REQUEST_HEADER_PREFIX = "mdc.requestHeader.";

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // do nothing
    }

    @Override
    public void doFilter(
            ServletRequest request, ServletResponse response, FilterChain chain
    ) throws IOException, ServletException {
        MDC.put(START_TIME, DateTime.now(DateTimeZone.UTC).toString(DATE_TIME_STRING_FORMAT));
        insertIntoMDC(request);
        try {
            chain.doFilter(request, response);
            MDC.put(END_TIME, DateTime.now(DateTimeZone.UTC).toString(DATE_TIME_STRING_FORMAT));
            MDC.put(STATUS, ((HttpServletResponse) response).getStatus() + "");
        } finally {
            clearMDC();
        }
    }

    @Override
    public void destroy() {
        // do nothing
    }

    protected void insertIntoMDC(final ServletRequest request) {
        MDC.put(REQUEST_ID_IDENTIFIER, UUID.randomUUID().toString().replaceAll("-", ""));
        MDC.put(ClassicConstants.REQUEST_REMOTE_HOST_MDC_KEY, request.getRemoteHost());

        if (request instanceof HttpServletRequest) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            MDC.put(ClassicConstants.REQUEST_REQUEST_URI, httpServletRequest.getRequestURI());
            final StringBuffer requestURL = httpServletRequest.getRequestURL();
            if (requestURL != null) {
                MDC.put(ClassicConstants.REQUEST_REQUEST_URL, requestURL.toString());
            }

            // These have special meaning for the Google V1 RequestLog
            MDC.put(ClassicConstants.REQUEST_METHOD, httpServletRequest.getMethod());
            MDC.put(ClassicConstants.REQUEST_QUERY_STRING, httpServletRequest.getQueryString());
            MDC.put(REQUEST_USER_AGENT_MDC_KEY, httpServletRequest.getHeader("User-Agent"));
            MDC.put(HTTP_VERSION, httpServletRequest.getProtocol());
            MDC.put(RESOURCE, httpServletRequest.getRequestURI());

            this.insertAllRequestHeaders(httpServletRequest);
        }
    }

    /**
     * Insert all request headers into the MDC for logging...
     *
     * @param servletRequest An instance of {@link HttpServletRequest} for the current request.
     */
    private void insertAllRequestHeaders(final HttpServletRequest servletRequest) {
        final List<String> headerNames = Collections.list(servletRequest.getHeaderNames());
        for (final String headerName : headerNames) {
            final StringBuilder headerValues = new StringBuilder();
            final List<String> headerValuesForName = Collections.list(servletRequest.getHeaders(headerName));

            boolean first = true;
            for (final String headerValue : headerValuesForName) {
                if (!first) {
                    headerValues.append("; ");
                }
                headerValues.append(headerValue);
            }
            MDC.put(REQUEST_HEADER_PREFIX + headerName, headerValues.toString());
        }
    }

    protected void clearMDC() {
        MDC.clear();
    }

}
