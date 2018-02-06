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

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.core.clients.versioncontrol.ClientLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueue;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueueOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Updates the local version information for a workspace to a specific version
 * 
 * This task basically fakes the server into believing that we have the
 * specified version of the file, without having to download all the files from
 * the server
 * 
 * Sub classes need to implement the methods required to build the GetOps needed
 * to update the server local version information
 */
public abstract class UpdateLocalVersionTask
    extends Task
{
    private static final Log log = LogFactory.getLog(UpdateLocalVersionTask.class);

    protected final Workspace workspace;

    /**
     * Constructor
     * 
     * @param workspace
     *        workspace to update
     */
    public UpdateLocalVersionTask(final Workspace workspace)
    {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;
    }

    protected abstract GetOperation[][] getGetOperations();

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor)
    {
        progressMonitor.beginTask(Messages.getString("UpdateLocalVersionTask.UpdatingLocalVersions"), //$NON-NLS-1$
            TaskProgressMonitor.INDETERMINATE);

        /* Retrieves the GetOps */
        GetOperation[][] tfsGetOperations = getGetOperations();

        if (tfsGetOperations == null)
        {
            /* There is nothing to update */

            return TaskStatus.OK_STATUS;
        }

        /*
         * Build the local version updates array to queue all the updates using
         * a single server call
         */
        ArrayList<ClientLocalVersionUpdate> localVersionUpdates = new ArrayList<ClientLocalVersionUpdate>();

        for (int i = 0; i < tfsGetOperations.length; i++)
        {
            for (int j = 0; j < tfsGetOperations[i].length; j++)
            {
                GetOperation getOp = tfsGetOperations[i][j];

                localVersionUpdates.add(new ClientLocalVersionUpdate(
                    getOp.getSourceServerItem(),
                    getOp.getItemID(),
                    getOp.getTargetLocalItem(),
                    getOp.getVersionServer(),
                    getOp.getPropertyValues()));
            }
        }

        /* Update the local version information using the Update queue */
        UpdateLocalVersionQueue queue = null;

        try
        {
            log.info("Calling server to update local versions"); //$NON-NLS-1$

            queue = new UpdateLocalVersionQueue(workspace, UpdateLocalVersionQueueOptions.UPDATE_SERVER);

            for (int count = 0; count < localVersionUpdates.size(); count++)
            {
                queue.queueUpdate(localVersionUpdates.get(count));
            }

            queue.flush();

            progressMonitor.endTask();
        }
        catch (Exception e)
        {
            return new TaskStatus(TaskStatus.ERROR, e);
        }
        finally
        {
            if (queue != null)
            {
                queue.close();
            }
        }

        return TaskStatus.OK_STATUS;
    }
}
