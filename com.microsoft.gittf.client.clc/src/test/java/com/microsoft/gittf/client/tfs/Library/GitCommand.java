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

import java.io.IOException;

import com.microsoft.gittf.client.tfs.TestEnvironment;
import com.microsoft.gittf.client.tfs.TestEnvironmentConstants;

/**
 * 
 * Class to wrap the git command itself.
 * 
 * @author jpresto
 * 
 */
public class GitCommand
    extends CommandBase
{
    private String workingFolder = null;
    private String args = null;

    public GitCommand(String programArgs)
        throws InvalidConfigurationException
    {
        args = programArgs;

        addEnvironmentVariable(
            TestEnvironmentConstants.VARIABLEJAVAHOME,
            TestEnvironment.getTestVariableValue(TestEnvironmentConstants.VARIABLEJAVAHOME));

        addEnvironmentPath(TestEnvironment.getGitExeFolder());
        addEnvironmentPath(TestEnvironment.getGitTfExeFolder());
    }

    @Override
    public String getExeFullPath()
    {
        return TestEnvironment.getGitExeFullPath();
    }

    @Override
    public String getExeFolder()
    {
        return TestEnvironment.getGitExeFolder();
    }

    @Override
    public int runCommand()
        throws IOException,
            InterruptedException
    {
        return super.run();
    }

    @Override
    public String getProcessArgs()
    {
        return args;
    }

    @Override
    public String getWorkingFolder()
    {
        return workingFolder;
    }

    @Override
    public void getWorkingFolder(String folder)
    {
        workingFolder = folder;
    }
}
