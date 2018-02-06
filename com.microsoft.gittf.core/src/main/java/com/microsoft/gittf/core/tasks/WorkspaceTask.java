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
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.lib.Repository;

import com.microsoft.gittf.core.interfaces.WorkspaceService;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskExecutor;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

/**
 * Base task for the tasks that need to create and maintain a workspace object.
 * 
 */
public abstract class WorkspaceTask
    extends Task
{
    private final Log log = LogFactory.getLog(this.getClass());

    private WorkspaceInfo workspaceData;

    protected final Repository repository;
    protected final VersionControlClient versionControlClient;
    protected final String serverPath;

    /**
     * Constructor
     * 
     * @param repository
     * @param versionControlClient
     * @param serverPath
     */
    protected WorkspaceTask(
        final Repository repository,
        final VersionControlClient versionControlClient,
        final String serverPath)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(versionControlClient, "versionControlClient"); //$NON-NLS-1$
        Check.notNullOrEmpty(serverPath, "serverPath"); //$NON-NLS-1$

        this.repository = repository;
        this.versionControlClient = versionControlClient;
        this.serverPath = serverPath;
    }

    /**
     * Creates a workspace
     * 
     * @param progressMonitor
     * @return
     * @throws Exception
     */
    protected WorkspaceInfo createWorkspace(final TaskProgressMonitor progressMonitor)
        throws Exception
    {
        return createWorkspace(progressMonitor, false);
    }

    /**
     * Creates a workspace
     * 
     * @param progressMonitor
     * @param previewOnly
     * @return
     * @throws Exception
     */
    protected WorkspaceInfo createWorkspace(final TaskProgressMonitor progressMonitor, boolean previewOnly)
        throws Exception
    {
        return createWorkspace(progressMonitor, previewOnly, null);
    }

    /**
     * Creates a workspace and sets the version of the items in the workspace to
     * the versionSpec specified
     * 
     * @param progressMonitor
     * @param previewOnly
     * @param versionSpec
     * @return
     * @throws Exception
     */
    protected WorkspaceInfo createWorkspace(
        final TaskProgressMonitor progressMonitor,
        boolean previewOnly,
        VersionSpec versionSpec)
        throws Exception
    {
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        /*
         * If the workspace has already been created return that workspace
         * object
         */
        if (workspaceData == null)
        {
            /* Create workspace task */
            final CreateWorkspaceTask createTask =
                new CreateWorkspaceTask(versionControlClient, serverPath, repository);

            createTask.setPreview(previewOnly);
            createTask.setVersionSpec(versionSpec);

            final TaskStatus createStatus = new TaskExecutor(progressMonitor).execute(createTask);

            if (!createStatus.isOK() && createStatus.getException() != null)
            {
                throw createStatus.getException();
            }
            else if (!createStatus.isOK())
            {
                throw new Exception(createStatus.getMessage());
            }

            workspaceData = new WorkspaceInfo(createTask.getWorkspace(), createTask.getWorkingFolder());
        }

        return workspaceData;
    }

    /**
     * Clean up the workspace object
     * 
     * @param progressMonitor
     */
    protected void disposeWorkspace(final TaskProgressMonitor progressMonitor)
    {
        TaskStatus deleteWorkspaceStatus = TaskStatus.OK_STATUS;

        if (workspaceData != null)
        {
            try
            {
                /* Delete the workspace task */
                deleteWorkspaceStatus =
                    new TaskExecutor(progressMonitor).execute(new DeleteWorkspaceTask(
                        workspaceData.getWorkspace(),
                        workspaceData.getWorkingFolder()));
            }
            finally
            {
                workspaceData = null;
            }
        }

        if (!deleteWorkspaceStatus.isOK())
        {
            log.warn(MessageFormat.format("Could not delete workspace: {0}", deleteWorkspaceStatus.getMessage())); //$NON-NLS-1$
        }
    }

    protected static final class WorkspaceInfo
    {
        private final WorkspaceService workspace;
        private final File workingFolder;

        private WorkspaceInfo(final WorkspaceService workspace, final File workingFolder)
        {
            Check.notNull(workspace, "workspace"); //$NON-NLS-1$
            Check.notNull(workingFolder, "workingFolder"); //$NON-NLS-1$

            this.workspace = workspace;
            this.workingFolder = workingFolder;
        }

        public WorkspaceService getWorkspace()
        {
            return workspace;
        }

        public File getWorkingFolder()
        {
            return workingFolder;
        }
    }
}
