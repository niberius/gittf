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

package com.microsoft.gittf.core.tasks;

import java.io.File;

import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.interfaces.WorkspaceService;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskProgressDisplay;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.util.FileHelpers;

public class DeleteWorkspaceTask
    extends Task
{
    private final WorkspaceService workspace;
    private final File workingFolder;

    public DeleteWorkspaceTask(WorkspaceService workspace, File workingFolder)
    {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(workingFolder, "workingFolder"); //$NON-NLS-1$

        this.workspace = workspace;
        this.workingFolder = workingFolder;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor)
    {
        Exception exception = null;

        progressMonitor.beginTask(
            Messages.getString("DeleteWorkspaceTask.DeletingWorkspace"), TaskProgressMonitor.INDETERMINATE, TaskProgressDisplay.DISPLAY_PROGRESS); //$NON-NLS-1$

        try
        {
            workspace.deleteWorkspace();
        }
        catch (Exception e)
        {
            exception = e;
        }

        try
        {
            if (workingFolder.exists())
            {
                FileHelpers.deleteDirectory(workingFolder);
            }
        }
        catch (Exception e)
        {
            exception = e;
        }

        progressMonitor.endTask();

        return (exception == null) ? TaskStatus.OK_STATUS : new TaskStatus(TaskStatus.ERROR, exception);
    }
}
