package com.kumuluz.ee.metrics;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet filter, which disables access to the metrics endpoint.
 *
 * @author Aljaž Blažej, Urban Malc
 */
public class KumuluzEEServletFilter implements Filter {

    private static final String DEBUG_KEY = "kumuluzee.debug";
    private static boolean debugMode;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();
        debugMode = configurationUtil.getBoolean(DEBUG_KEY).orElse(false);
        ConfigurationUtil.getInstance().subscribe(DEBUG_KEY, (String key, String value) -> {
            if (DEBUG_KEY.equals(key)) {
                if ("true".equals(value.toLowerCase())) {
                    debugMode = true;
                } else {
                    debugMode = false;
                }
            }
        });
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (debugMode) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            response.sendError(403);
            filterChain.doFilter(servletRequest, response);
        }
    }

    @Override
    public void destroy() {
    }
}
