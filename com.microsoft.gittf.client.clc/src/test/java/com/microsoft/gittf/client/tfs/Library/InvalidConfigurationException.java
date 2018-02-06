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

package com.microsoft.gittf.client.tfs.Library;

import java.text.MessageFormat;

/**
 * 
 * Exception for invalid configuration or lack of a configuration.
 * 
 * @author jpresto
 * 
 */
public class InvalidConfigurationException
    extends Exception
{
    private static final long serialVersionUID = 1L;

    public InvalidConfigurationException(String message)
    {
        super(MessageFormat.format("{0}\n{1}\n{2}\n", message, HELP, SAMPLETESTFILE)); //$NON-NLS-1$
    }

    private static final String SAMPLETESTFILE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" //$NON-NLS-1$
        + "<TestEnvironment>\n" //$NON-NLS-1$
        + "  <TfsDeployment>\n" //$NON-NLS-1$
        + "    <ConfigurationServerInput Url=\"http://localhost:8080/tfs\">\n" //$NON-NLS-1$
        + "      <ProjectCollectionInput Name=\"TestDefault\">\n" //$NON-NLS-1$
        + "        <TeamProjectInput Name=\"DefaultAgileProject\" Tag=\"DefaultAgileProject\" />\n" //$NON-NLS-1$
        + "      </ProjectCollectionInput>\n" //$NON-NLS-1$
        + "    </ConfigurationServerInput>\n" //$NON-NLS-1$
        + "  </TfsDeployment>\n" //$NON-NLS-1$
        + "  <TestVariables>\n" //$NON-NLS-1$
        + "    <Value Key=\"GitExePath\">C:\\Program Files (x86)\\Git\\cmd</Value>\n" //$NON-NLS-1$
        + "    <Value Key=\"GitTfExePath\">C:\\git-tf\\git-tf-1.0.1.SNAPSHOT</Value>\n" //$NON-NLS-1$
        + "    <Value Key=\"JavaHome\">C:\\Program Files\\Java\\jdk1.7.0_07</Value>\n" //$NON-NLS-1$
        + "    <Value Key=\"HttpProxy\">http://itgproxy.redmond.corp.microsoft.com</Value>\n" //$NON-NLS-1$
        + "  <Value Key=\"GitRepositoryRootPath\">G:\\_TEST</Value>\n" //$NON-NLS-1$
        + "  </TestVariables>\n" //$NON-NLS-1$
        + "</TestEnvironment>  \n"; //$NON-NLS-1$

    private static final String HELP =
        "Be sure to have a valid TestEnviornment.xml file and have it either " //$NON-NLS-1$
            + "in c:\\ or an environment variable named 'TestConfiguraitonFile' pointing to where the test configuration file " //$NON-NLS-1$
            + "is located.  Below is a sample configuration file; be sure the collection and the team project " //$NON-NLS-1$
            + "already exists (these tests do not create TFS collections or team projects"; //$NON-NLS-1$
}
