package com.sappenin.gcp.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Unit test to experiement with logging formats.
 */
public class TestHarness {

    private MockHttpServletRequest mockHttpServletRequest;
    private MockHttpServletResponse mockHttpServletResponse;
    private MockFilterChain mockFilterChain;

    private MDCInsertingServletFilter filter;

    @Before
    public void setup() throws IOException, ServletException {
        this.filter = new MDCInsertingServletFilter();

        this.mockHttpServletRequest = new MockHttpServletRequest("GET", "/test-url");
        mockHttpServletRequest.addHeader("foo", "bar");
        mockHttpServletRequest.addHeader("User-Agent", "iPhone");
        mockHttpServletRequest.addHeader("X-Forwarded-For", "https://www.example.com");
        mockHttpServletRequest.addHeader("X-Example-Header", "baz");

        this.mockHttpServletResponse = new MockHttpServletResponse();
        mockHttpServletResponse.setStatus(503);
    }

    @After
    public void cleanup() {
        filter.clearMDC();
    }

    @Test
    public void testLayout() throws IOException, ServletException {
        final Logger logger = LoggerFactory.getLogger("CoolClass2");

        this.mockFilterChain = new MockFilterChain(new Servlet() {
            @Override
            public void init(ServletConfig config) throws ServletException {
                logger.debug("#init");
            }

            @Override
            public ServletConfig getServletConfig() {
                logger.debug("#getServletConfig");
                return null;
            }

            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
                logger.debug("#service");
                logger.info("About to do something...");
            }

            @Override
            public String getServletInfo() {
                logger.debug("#getServletInfo");
                return null;
            }

            @Override
            public void destroy() {
                logger.debug("#destroy");
            }
        }, filter);

        mockFilterChain.doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

}
