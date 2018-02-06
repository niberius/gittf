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

package com.microsoft.gittf.client.tfs;

/**
 * Constants for reading the test configuration file.
 * 
 * @author jpresto
 * 
 */
public class TestEnvironmentConstants
{
    public static final String TESTVARIABLES = "TestVariables"; //$NON-NLS-1$

    public static final String VARIABLEGITEXEPATH = "GitExePath"; //$NON-NLS-1$
    public static final String VARIABLEGITTFEXEPATH = "GitTfExePath"; //$NON-NLS-1$
    public static final String VARIABLEJAVAHOME = "JavaHome"; //$NON-NLS-1$
    public static final String VARIABLEHTTPPROXY = "HttpProxy"; //$NON-NLS-1$
    public static final String VARIBLEGITREPOSITORYROOTPATH = "GitRepositoryRootPath"; //$NON-NLS-1$

    public static final String CONFIGURATIONSERVERINPUT = "ConfigurationServerInput"; //$NON-NLS-1$
    public static final String PROJECTCOLLECTIONINPUT = "ProjectCollectionInput"; //$NON-NLS-1$
    public static final String NAME = "Name"; //$NON-NLS-1$
    public static final String VALUE = "Value"; //$NON-NLS-1$
    public static final String KEY = "Key"; //$NON-NLS-1$
    public static final String TEAMPROJECTINPUT = "TeamProjectInput"; //$NON-NLS-1$

    public static final String GITEXE = "git.exe"; //$NON-NLS-1$
    public static final String GITTFEXE = "git-tf.cmd"; //$NON-NLS-1$

    public static final String PATH = "PATH"; //$NON-NLS-1$

    public static final String GetNewLine()
    {
        return System.getProperty("line.separator"); //$NON-NLS-1$
    }
}
