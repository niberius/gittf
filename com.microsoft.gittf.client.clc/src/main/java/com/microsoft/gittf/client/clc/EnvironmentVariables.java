/***********************************************************************************************
 * Copyright (c) Microsoft Corporation All rights reserved.
 * 
 * MIT License:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ***********************************************************************************************/

package com.microsoft.gittf.client.clc;

/**
 * Environment variables used by git-tf
 * 
 */
public final class EnvironmentVariables
{
    /**
     * Specifies the value to use as the HTTP <em>and</em> HTTPS proxy for all
     * connections. Upper-case and lower-case variables are supported.
     * <p>
     * The default (when this variable is not set) is to use the operating
     * system's proxy, if it can be detected, otherwise no proxy is used.
     */
    public static final String HTTP_PROXY_URL = "HTTP_PROXY"; //$NON-NLS-1$
    public static final String HTTP_PROXY_URL_ALTERNATE = "http_proxy"; //$NON-NLS-1$

    /**
     * Specifies the value to use as the HTTPS proxy for all connections.
     * Upper-case and lower-case variables are supported.
     * <p>
     * The default (when this variable is not set) is to use the operating
     * system's proxy, if it can be detected, otherwise no proxy is used.
     */
    public static final String HTTPS_PROXY_URL = "HTTPS_PROXY"; //$NON-NLS-1$
    public static final String HTTPS_PROXY_URL_ALTERNATE = "https_proxy"; //$NON-NLS-1$

    /**
     * Specifies the list of hosts / domain names that will <em>not</em> be
     * subject to the {@link HTTP_PROXY_URL} environment variable (above.)
     */
    public static final String NO_PROXY_HOSTS = "NO_PROXY"; //$NON-NLS-1$
    public static final String NO_PROXY_HOSTS_ALTERNATE = "no_proxy"; //$NON-NLS-1$

    private EnvironmentVariables()
    {
    }
}
