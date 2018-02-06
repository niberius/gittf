/*
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
 */

package com.microsoft.gittf.client.clc.connection;

import com.microsoft.gittf.client.clc.EnvironmentVariables;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.IllegalConfigurationException;
import com.microsoft.tfs.core.config.httpclient.DefaultHTTPClientFactory;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.*;
import com.microsoft.tfs.core.httpclient.auth.AuthScope;
import com.microsoft.tfs.core.httpclient.protocol.Protocol;
import com.microsoft.tfs.core.util.TFSUsernameParseException;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.CollatorFactory;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.MessageFormat;

public class GitTFHTTPClientFactory
        extends DefaultHTTPClientFactory {
    private static final Log log = LogFactory.getLog(GitTFHTTPClientFactory.class);

    private final CredentialsManager credentialsManager;

    public GitTFHTTPClientFactory(
            final ConnectionInstanceData connectionInstanceData,
            final CredentialsManager credentialsManager) {
        super(connectionInstanceData);

        Check.notNull(credentialsManager, "credentialsManager");
        this.credentialsManager = credentialsManager;
    }

    /**
     * Determines whether the given host should be proxied or not, based on the
     * pipe-separated list of wildcards to not proxy (generally taken from the
     * <code>http.nonProxyHosts</code> system property.)
     *
     * @param serverURI     the host to query (not <code>null</code>)
     * @param nonProxyHosts the pipe-separated list of hosts (or wildcards) that should not be
     *                      proxied, or <code>null</code> if all hosts are proxied
     * @return <code>true</code> if the host should be proxied,
     * <code>false</code> otherwise
     */
    static boolean hostExcludedFromProxyProperties(URI serverURI, String nonProxyHosts) {
        if (serverURI == null || serverURI.getHost() == null || nonProxyHosts == null) {
            return false;
        }

        for (String nonProxyHost : nonProxyHosts.split("\\|"))
        {
            /*
             * Note: for wildcards, the java specification says that the host
             * "may start OR end with a *" (emphasis: mine).
             */
            if (nonProxyHost.startsWith("*") && LocaleInvariantStringHelpers.caseInsensitiveEndsWith(serverURI.getHost(), nonProxyHost.substring(1)))
            {
                return true;
            } else if (nonProxyHost.endsWith("*") && LocaleInvariantStringHelpers.caseInsensitiveStartsWith(serverURI.getHost(), nonProxyHost.substring(0, nonProxyHost.length() - 1)))
            {
                return true;
            } else if (CollatorFactory.getCaseInsensitiveCollator().equals(serverURI.getHost(), nonProxyHost)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines whether the given host should be proxied or not, based on the
     * comma-separated list of wildcards to not proxy (generally taken from the
     * <code>http.nonProxyHosts</code> system property.
     *
     * @param serverURI     the host to query (not <code>null</code>)
     * @param nonProxyHosts the pipe-separated list of hosts (or wildcards) that should not be
     *                      proxied, or <code>null</code> if all hosts are proxied
     * @return <code>true</code> if the host should be proxied,
     * <code>false</code> otherwise
     */
    static boolean hostExcludedFromProxyEnvironment(URI serverURI, String nonProxyHosts) {
        if (serverURI == null || serverURI.getHost() == null || nonProxyHosts == null) {
            return false;
        }

        nonProxyHosts = nonProxyHosts.trim();
        if (nonProxyHosts.length() == 0) {
            return false;
        }

        /*
         * The no_proxy setting may be '*' to indicate nothing is proxied.
         * However, this is the only allowable use of a wildcard.
         */
        if ("*".equals(nonProxyHosts))
        {
            return true;
        }

        String serverHost = serverURI.getHost();

        /* Map default ports to the appropriate default. */
        int serverPort = serverURI.getPort();

        if (serverPort == -1) {
            try {
                serverPort = Protocol.getProtocol(serverURI.getScheme().toLowerCase()).getDefaultPort();
            } catch (IllegalStateException e) {
                serverPort = 80;
            }
        }

        for (String nonProxyHost : nonProxyHosts.split(","))
        {
            int nonProxyPort = -1;

            if (nonProxyHost.contains(":"))
            {
                String[] nonProxyParts = nonProxyHost.split(":", 2);

                nonProxyHost = nonProxyParts[0];

                try {
                    nonProxyPort = Integer.parseInt(nonProxyParts[1]);
                } catch (Exception e) {
                    log.warn(MessageFormat.format(
                            "Could not parse port in non_proxy setting: {0}, ignoring port", nonProxyParts[1]));
                }
            }

            /*
             * If the no_proxy entry specifies a port, match it exactly. If it
             * does not, this means to match all ports.
             */
            if (nonProxyPort != -1 && serverPort != nonProxyPort) {
                continue;
            }

            /*
             * Otherwise, the nonProxyHost portion is treated as the trailing
             * DNS entry
             */
            if (LocaleInvariantStringHelpers.caseInsensitiveEndsWith(serverHost, nonProxyHost)) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureClientProxy(
            final HttpClient httpClient,
            final HostConfiguration hostConfiguration,
            final HttpState httpState,
            final ConnectionInstanceData connectionInstanceData) {
        CLCHTTPProxyConfiguration proxyConfiguration = null;

        /*
         * Try using the http.proxyHost / https.proxyHost system properties.
         * These must have been configured for our application by editing the
         * launcher (on Unix/Windows systems) or were set by the JVM from system
         * defaults. (on Mac OS)
         */
        proxyConfiguration =
                configureClientProxyFromProperties(httpClient, hostConfiguration, httpState, connectionInstanceData);

        /*
         * Try environment variables.
         */
        if (proxyConfiguration == null) {
            proxyConfiguration =
                    configureClientProxyFromEnvironment(httpClient, hostConfiguration, httpState, connectionInstanceData);
        }

        /*
         * Return early if still no proxy configured.
         */
        if (proxyConfiguration == null) {
            final String message =
                    MessageFormat.format("Environment variables {0},{1} not set, no global proxy configured",
                            EnvironmentVariables.HTTP_PROXY_URL,
                            EnvironmentVariables.HTTP_PROXY_URL_ALTERNATE);
            log.debug(message);
            return;
        }

        final String message = MessageFormat.format("Using global proxy URL {0}:{1}",
                proxyConfiguration.getHost(),
                Integer.toString(proxyConfiguration.getPort()));
        log.debug(message);

        hostConfiguration.setProxy(proxyConfiguration.getHost(), proxyConfiguration.getPort());

        if (proxyConfiguration.getUsername() != null && proxyConfiguration.getPassword() != null) {
            String username;

            try {
                username = proxyConfiguration.getUsername();
            } catch (TFSUsernameParseException e) {
                log.warn("Unable to determine domain from proxy username", e);
                username = proxyConfiguration.getUsername();
            }

            if (username != null) {
                httpState.setProxyCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                        username,
                        proxyConfiguration.getPassword()));
            }
        } else {
            httpState.setProxyCredentials(AuthScope.ANY, new DefaultNTCredentials());
        }
    }

    private CLCHTTPProxyConfiguration configureClientProxyFromProperties(
            final HttpClient httpClient,
            final HostConfiguration hostConfiguration,
            final HttpState httpState,
            final ConnectionInstanceData connectionInstanceData) {
        String proxyHost = null;
        String proxyPort = null;
        String nonProxyHosts = null;

        if ("http".equalsIgnoreCase(connectionInstanceData.getServerURI().getScheme()))
        {
            proxyHost = System.getProperty("http.proxyHost");
            proxyPort = System.getProperty("http.proxyPort");
            nonProxyHosts = System.getProperty("http.nonProxyHosts");
        } else if ("https".equalsIgnoreCase(connectionInstanceData.getServerURI().getScheme()))
        {
            proxyHost = System.getProperty("https.proxyHost");
            proxyPort = System.getProperty("https.proxyPort");
            nonProxyHosts = System.getProperty("https.nonProxyHosts");
        }

        if (proxyHost != null
                && proxyHost.length() > 0
                && !hostExcludedFromProxyProperties(connectionInstanceData.getServerURI(), nonProxyHosts)) {
            int proxyPortValue = -1;

            if (proxyPort != null && proxyPort.length() > 0) {
                try {
                    proxyPortValue = Integer.parseInt(proxyPort);
                } catch (NumberFormatException e) {
                    log.warn(MessageFormat.format("Could not parse proxy port {0}, using default", proxyPort), e);
                }
            }

            try {
                URI proxyURI = new URI("http", null, proxyHost, proxyPortValue, "/", null, null);

                /* Make sure proxy host is well-formed */
                if (proxyURI.getHost() == null) {
                    final String messageFormat =
                            Messages.getString("GitTFHTTPClientFactory.ProxyURLDoesNotContainValidHostnameFormat");
                    final String message = MessageFormat.format(messageFormat, proxyURI.toString());
                    log.warn(message);
                    throw new IllegalConfigurationException(message);
                }

                /* See if we have credentials cached for this proxy */
                CachedCredentials proxyCredentials = credentialsManager.getCredentials(proxyURI);

                String username = proxyCredentials != null ? proxyCredentials.getUsername() : null;
                String password = proxyCredentials != null ? proxyCredentials.getPassword() : null;

                return new CLCHTTPProxyConfiguration(proxyHost, proxyPortValue, username, password);
            } catch (URISyntaxException e) {
                log.warn("Could not parse proxy URI, proxy will not be configured", e);
            }
        }

        return null;
    }

    private CLCHTTPProxyConfiguration configureClientProxyFromEnvironment(
            final HttpClient httpClient,
            final HostConfiguration hostConfiguration,
            final HttpState httpState,
            final ConnectionInstanceData connectionInstanceData) {
        String proxyUrl = null;
        String nonProxyHosts;

        /*
         * If we're doing HTTPS, check for the presence of an HTTPS proxy
         * environment variable.
         */
        if ("https".equalsIgnoreCase(connectionInstanceData.getServerURI().getScheme()))
        {
            proxyUrl = PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.HTTPS_PROXY_URL);

            if (proxyUrl == null || proxyUrl.length() == 0) {
                proxyUrl =
                        PlatformMiscUtils.getInstance().getEnvironmentVariable(
                                EnvironmentVariables.HTTPS_PROXY_URL_ALTERNATE);
            }
        }

        /*
         * Check for the presence of an HTTP proxy environment variable and use
         * that as the global proxy. (lynx documented the environment variable
         * as lower case "http_proxy", so we need to check both the variable and
         * its alternate.)
         *
         * (Note, we have always tried using the HTTP_PROXY environment variable
         * for HTTPS connections, so continue to support this.)
         */
        if (proxyUrl == null || proxyUrl.length() == 0) {
            proxyUrl = PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.HTTP_PROXY_URL);

            if (proxyUrl == null || proxyUrl.length() == 0) {
                proxyUrl =
                        PlatformMiscUtils.getInstance().getEnvironmentVariable(
                                EnvironmentVariables.HTTP_PROXY_URL_ALTERNATE);
            }
        }

        if (proxyUrl == null || proxyUrl.length() == 0) {
            return null;
        }

        /*
         * Check against the NO_PROXY environment variable. (lynx also
         * documented "no_proxy" as lower case here, so we need to check both
         * the variable and its alternate.)
         */
        nonProxyHosts = PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.NO_PROXY_HOSTS);

        if (nonProxyHosts == null || nonProxyHosts.length() == 0) {
            nonProxyHosts =
                    PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.NO_PROXY_HOSTS_ALTERNATE);
        }

        if (hostExcludedFromProxyEnvironment(connectionInstanceData.getServerURI(), nonProxyHosts)) {
            return null;
        }

        URI proxyURI;

        try {
            proxyURI = new URI(proxyUrl);
        } catch (URISyntaxException e) {
            final String messageFormat = Messages.getString("GitTFHTTPClientFactory.IllegalProxyURLFormat");
            final String message = MessageFormat.format(messageFormat, proxyUrl);
            log.warn(message, e);
            throw new IllegalConfigurationException(message, e);
        }

        if (proxyURI.getHost() == null) {
            final String messageFormat = Messages.getString("GitTFHTTPClientFactory.IllegalProxyURLFormat");
            final String message = MessageFormat.format(messageFormat, proxyUrl);
            log.warn(message);
            throw new IllegalConfigurationException(message);
        }

        String username = null, password = null;
        if (proxyURI.getRawUserInfo() != null) {
            String[] userInfo = proxyURI.getRawUserInfo().split(":", 2);

            try {
                username = URLDecoder.decode(userInfo[0], "UTF-8");
                password = URLDecoder.decode(userInfo[1], "UTF-8");
            } catch (Exception e) {
                log.warn("Could not decode user info as UTF-8", e);
            }
        } else {
            /*
             * If the proxy credentials were NOT specified in the URI itself,
             * look up the credentials
             */
            CachedCredentials proxyCredentials = credentialsManager.getCredentials(proxyURI);

            username = proxyCredentials != null ? proxyCredentials.getUsername() : null;
            password = proxyCredentials != null ? proxyCredentials.getPassword() : null;
        }

        return new CLCHTTPProxyConfiguration(proxyURI.getHost(), proxyURI.getPort(), username, password);
    }

    private static class CLCHTTPProxyConfiguration {
        private final String host;
        private final int port;

        private final String username;
        private final String password;

        public CLCHTTPProxyConfiguration(String host, int port, String username, String password) {
            Check.notNull(host, "host");

            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
