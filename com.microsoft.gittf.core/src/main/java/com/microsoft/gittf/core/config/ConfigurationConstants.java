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

package com.microsoft.gittf.core.config;

import com.microsoft.gittf.core.GitTFConstants;

/**
 * Constants used by the configuration classes
 * 
 */
public class ConfigurationConstants
{
    public static final String CONFIGURATION_SECTION = GitTFConstants.GIT_TF_NAME;

    public static final String GENERAL_SUBSECTION = null;
    public static final String DEPTH = "depth"; //$NON-NLS-1$
    public static final String TAG = "tag"; //$NON-NLS-1$
    public static final String INCLUDE_METADATA = "include-metadata"; //$NON-NLS-1$
    public static final String FILE_FORMAT_VERSION = "file-format-version"; //$NON-NLS-1$
    public static final String TEMP_DIRECTORY = "tempdir"; //$NON-NLS-1$
    public static final String KEEP_AUTHOR = "keep-author"; //$NON-NLS-1$
    public static final String USER_MAP = "user-map"; //$NON-NLS-1$

    public static final String SERVER_SUBSECTION = "server"; //$NON-NLS-1$
    public static final String SERVER_COLLECTION_URI = "collection"; //$NON-NLS-1$
    public static final String SERVER_PATH = "serverpath"; //$NON-NLS-1$
    public static final String USERNAME = "username"; //$NON-NLS-1$
    public static final String PASSWORD = "password"; //$NON-NLS-1$
    public static final String GATED_BUILD_DEFINITION = "gated"; //$NON-NLS-1$

    public static final String COMMIT_SUBSECTION = "commits"; //$NON-NLS-1$
    public static final String COMMIT_CHANGESET_FORMAT = "changeset-{0}"; //$NON-NLS-1$
    public static final String CHANGESET_SUBSECTION = "changesets"; //$NON-NLS-1$
    public static final String CHANGESET_COMMIT_FORMAT = "commit-{0}"; //$NON-NLS-1$
    public static final String CHANGESET_HIGHWATER = "hwm"; //$NON-NLS-1$

    private ConfigurationConstants()
    {
    }
}
