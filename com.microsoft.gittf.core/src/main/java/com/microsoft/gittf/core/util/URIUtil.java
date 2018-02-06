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

package com.microsoft.gittf.core.util;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.gittf.core.Messages;

public final class URIUtil
{
    private static final String hostedServerName = "visualstudio.com"; //$NON-NLS-1$
    private static final String hostedServerPreviewName = "tfspreview.com"; //$NON-NLS-1$

    private static final String hostedServerScheme = "https"; //$NON-NLS-1$
    private static final String hostedServerPath = "/DefaultCollection"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(URIUtil.class);

    private URIUtil()
    {
    }

    /**
     * Converts the specified string into a valid server URI. The method will
     * update the URI if needed when connecting to the hosted service, to make
     * sure that the connection is HTTPS and is to the default collection
     * 
     * @param serverURIString
     *        the uri string to convert
     * @return
     * @throws Exception
     */
    public static URI getServerURI(final String serverURIString)
        throws Exception
    {
        try
        {
            URI uri = new URI(serverURIString);

            if (!uri.isAbsolute() || uri.isOpaque())
            {
                uri = null;
            }

            if (uri != null)
            {
                uri = updateIfNeededForHostedService(uri);
            }

            if (uri == null)
            {
                throw new Exception(Messages.formatString("URIUtil.InvalidURIFormat", serverURIString)); //$NON-NLS-1$
            }

            return uri;
        }
        catch (Exception e)
        {
            log.warn("Could not parse URI", e); //$NON-NLS-1$
        }

        throw new Exception(Messages.formatString("URIUtil.InvalidURIFormat", serverURIString)); //$NON-NLS-1$
    }

    /**
     * Ensures that git tf is connecting to the right hosted server scheme and
     * path.
     * 
     * @param uri
     * @return
     * @throws URISyntaxException
     */
    private static URI updateIfNeededForHostedService(URI uri)
        throws URISyntaxException
    {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        String hostedServer = System.getProperty("tfs.hosted"); //$NON-NLS-1$
        if (hostedServer == null || hostedServer.length() == 0)
        {
            hostedServer = hostedServerName;
        }

        if (uri.getHost().toLowerCase().contains(hostedServer)
            || uri.getHost().toLowerCase().contains(hostedServerPreviewName))
        {
            String uriPath = uri.getPath().replaceAll("[/]+$", ""); //$NON-NLS-1$ //$NON-NLS-2$
            if (uriPath == null || uriPath.length() == 0)
            {
                uriPath = hostedServerPath;
            }

            return new URI(hostedServerScheme, uri.getHost(), uriPath, null);
        }

        return uri;
    }
}
