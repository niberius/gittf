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

package com.microsoft.gittf.core;

/**
 * Constants used by the Git TF Library
 * 
 */
public final class GitTFConstants
{
    /**
     * The Application Name
     */
    public static final String GIT_TF_NAME = "git-tf"; //$NON-NLS-1$

    /**
     * The latest format version of the git tf configuration file
     */
    public static final int GIT_TF_CURRENT_FORMAT_VERSION = 1;

    /**
     * The root of the temporary directory to use
     */
    public static final String GIT_TF_DIRNAME = "tf"; //$NON-NLS-1$

    /**
     * The default depth option
     */
    public static final boolean GIT_TF_DEFAULT_DEEP = false;

    /**
     * The default shallow depth option
     */
    public static final int GIT_TF_SHALLOW_DEPTH = 1;

    /**
     * The default setting for including metadata in TFS changesets
     */
    public static final boolean GIT_TF_DEFAULT_INCLUDE_METADATA = false;

    /**
     * The name of the remote branch that maps to TFS
     */
    public static final String GIT_TF_REMOTE = "origin_tfs/"; //$NON-NLS-1$

    /**
     * The name of the branch to use that maps to TFS (Used only in bare
     * repository)
     */
    public static final String GIT_TF_BRANCHNAME = "tfs"; //$NON-NLS-1$

    /**
     * The default keep-author option
     */
    public static final boolean GIT_TF_DEFAULT_KEEP_AUTHOR = false;

    /**
     * The default file path for the user map
     */
    public static final String GIT_TF_DEFAULT_USER_MAP = "./USERMAP"; //$NON-NLS-1$

    private GitTFConstants()
    {
    }
}
