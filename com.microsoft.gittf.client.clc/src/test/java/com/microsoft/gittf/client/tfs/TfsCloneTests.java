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

import java.text.MessageFormat;

import com.microsoft.gittf.client.tfs.Library.GitTfCommand;
import com.microsoft.gittf.client.tfs.Library.Logger;

public class TfsCloneTests
    extends GitTfTestBase
{
    /**
     * Just clone a tfs database which was provided in the test configuration
     * file.
     */
    public void testGitTfClone()
    {
        if (!configured)
        {
            return;
        }

        try
        {
            GitTfCommand cmd =
                new GitTfCommand(MessageFormat.format("clone {0} $/", TestEnvironment.getCollectionUrl())); //$NON-NLS-1$
            cmd.getWorkingFolder(getWorkspaceFolder());
            cmd.run();

            Logger.log(MessageFormat.format("Command Input:  {0}", cmd.getCommandInput())); //$NON-NLS-1$
            Logger.log(MessageFormat.format("Exit:  {0}", cmd.getExitCode())); //$NON-NLS-1$
            Logger.log("Standard Output", cmd.getStandardOut()); //$NON-NLS-1$ 
            Logger.log("Standard Error", cmd.getStandardErr()); //$NON-NLS-1$

            if (cmd.getExitCode() != 0)
            {
                cmd.logDetails();
            }

            if (cmd.getStandardErr().length() != 0)
            {
                super.fail(MessageFormat.format("Expected no standard error but received: {0}", cmd.getStandardErr())); //$NON-NLS-1$
            }

            assertEquals(0, cmd.getExitCode());
        }
        catch (Throwable e)
        {
            fail(e.getMessage());
        }
    }
}
