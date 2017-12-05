/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper GmbH licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.http;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This IP filter class accepts a list of IPs for blacklisting OR for whitelisting (but not both).
 * <p>
 * Additionally to exact match a simple wildcard expression ala 1.2.3* or 1.*.3.4 is allowed.
 * <p>
 * The internal ip filter from jetty did not work (NP exceptions)
 * <p>
 *
 * @author Peter Karich
 */
public class AuthFilter implements Filter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
	private final String	token;

	public AuthFilter(String token) {
		this.token = token;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			
			if (token == null || token.equals(httpRequest.getHeader("giro-key"))) {
				chain.doFilter(request, response);
			} else {
				logger.warn("Rejecting request with wrong key: " + httpRequest.getHeader("giro-key"));
				((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		}
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
