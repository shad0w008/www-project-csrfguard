/*
 * The OWASP CSRFGuard Project, BSD License
 * Copyright (c) 2011, Eric Sheridan (eric@infraredsecurity.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice,
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of OWASP nor the names of its contributors may be used
 *        to endorse or promote products derived from this software without specific
 *        prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.owasp.csrfguard;

import org.owasp.csrfguard.http.InterceptRedirectResponse;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public final class CsrfGuardFilter implements Filter {

    private FilterConfig filterConfig = null;

    @Override
    public void destroy() {
        this.filterConfig = null;
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain filterChain) throws IOException, ServletException {

        // maybe the short circuit to disable is set
        if (!CsrfGuard.getInstance().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        /* only work with HttpServletRequest objects */
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {

            final HttpServletRequest httpRequest = (HttpServletRequest) request;
            final HttpSession session = httpRequest.getSession(false);

            // if there is no session and we aren't validating when no session exists
            if (session == null && !CsrfGuard.getInstance().isValidateWhenNoSessionExists()) {
                // If there is no session, no harm can be done
                filterChain.doFilter(httpRequest, response);
                return;
            }

            final CsrfGuard csrfGuard = CsrfGuard.getInstance();
            final InterceptRedirectResponse httpResponse = new InterceptRedirectResponse((HttpServletResponse) response, httpRequest, csrfGuard);

//			 if(MultipartHttpServletRequest.isMultipartRequest(httpRequest)) {
//				 httpRequest = new MultipartHttpServletRequest(httpRequest);
//			 }

            if ((session != null && session.isNew()) && csrfGuard.isUseNewTokenLandingPage()) {
                csrfGuard.writeLandingPage(httpRequest, httpResponse);
            } else if (csrfGuard.isValidRequest(httpRequest, httpResponse)) {
                filterChain.doFilter(httpRequest, httpResponse);
            } else {
                /*
                 TODO log
                 invalid request - nothing to do - actions already executed
                 */
            }

            /* update tokens */
            csrfGuard.updateTokens(httpRequest);

        } else {
            this.filterConfig.getServletContext().log(String.format("[WARNING] CSRFGuard does not know how to work with requests of class %s ", request.getClass().getName()));

            filterChain.doFilter(request, response);
        }
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }
}
