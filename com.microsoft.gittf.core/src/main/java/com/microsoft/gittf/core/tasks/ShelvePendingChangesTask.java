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

import java.util.Calendar;

import org.eclipse.jgit.lib.Repository;

import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.interfaces.WorkspaceService;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.util.NewlineUtils;
import com.microsoft.tfs.util.StringUtil;

/**
 * Shelves the changes pended in the workspace
 * 
 */
public class ShelvePendingChangesTask
    extends Task
{
    private static String CRLF = "" + NewlineUtils.CARRIAGE_RETURN + NewlineUtils.LINE_FEED; //$NON-NLS-1$
    private final String message;
    private final WorkspaceService workspace;
    private final PendingChange[] changes;
    private final String shelvesetName;

    private WorkItemCheckinInfo[] workItems;
    private boolean replace = false;

    /**
     * Constructor
     * 
     * @param repository
     *        the git repository
     * @param message
     *        the shelveset comment to use
     * @param workspace
     *        the workspace to use
     * @param changes
     *        the changes to shelve
     * @param shelvesetName
     *        the shelveset name
     */
    public ShelvePendingChangesTask(
        final Repository repository,
        final String message,
        final WorkspaceService workspace,
        final PendingChange[] changes,
        final String shelvesetName)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(changes, "changes"); //$NON-NLS-1$
        Check.isTrue(changes.length >= 1, "changes.length >= 1"); //$NON-NLS-1$
        Check.notNullOrEmpty(shelvesetName, "shelvesetName"); //$NON-NLS-1$

        this.message = message;
        this.workspace = workspace;
        this.changes = changes;
        this.shelvesetName = shelvesetName;
    }

    /**
     * Sets the work item info to associate
     * 
     * @param workItems
     */
    public void setWorkItemCheckinInfo(WorkItemCheckinInfo[] workItems)
    {
        this.workItems = workItems;
    }

    /**
     * Sets the flag to indicate that we could replace a shelveset on the server
     * with the same name. The default is false.
     * 
     * @param replace
     */
    public void setReplaceExistingShelveset(boolean replace)
    {
        this.replace = replace;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor)
    {
        progressMonitor.beginTask(
            Messages.formatString("ShelvePendingChangesTask.ShelvingChangesFormat", changes.length), TaskProgressMonitor.INDETERMINATE); //$NON-NLS-1$

        final String normalizedMessage =
            (message == null ? StringUtil.EMPTY : NewlineUtils.replaceNewlines(message, CRLF).trim());

        try
        {
            /* Create shelveset */
            final Shelveset shelveset =
                new Shelveset(
                    shelvesetName,
                    VersionControlConstants.AUTHENTICATED_USER,
                    VersionControlConstants.AUTHENTICATED_USER,
                    normalizedMessage,
                    null,
                    null,
                    workItems,
                    Calendar.getInstance(),
                    false,
                    null);

            workspace.shelve(shelveset, changes, replace, true);

            progressMonitor.endTask();
        }
        catch (Exception e)
        {
            return new TaskStatus(TaskStatus.ERROR, e);
        }

        return TaskStatus.OK_STATUS;
    }
}
