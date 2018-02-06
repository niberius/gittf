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

import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.impl.PreviewOnlyWorkspace;
import com.microsoft.gittf.core.impl.TfsWorkspace;
import com.microsoft.gittf.core.interfaces.WorkspaceService;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskExecutor;
import com.microsoft.gittf.core.tasks.framework.TaskProgressDisplay;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.DirectoryUtil;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.GUID;

public class CreateWorkspaceTask
    extends Task
{
    private final static Log log = LogFactory.getLog(CreateWorkspaceTask.class);

    private final VersionControlClient versionControlClient;
    private final Repository repository;
    private final String serverPath;

    private boolean updateLocalVersion = true;
    private boolean preview = false;
    private VersionSpec localVersionSpec = null;

    private WorkspaceService workspace;
    private File workingFolder;

    public CreateWorkspaceTask(
        final VersionControlClient versionControlClient,
        final String serverPath,
        final Repository repository)
    {
        Check.notNull(versionControlClient, "versionControlClient"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(serverPath, "serverPath"); //$NON-NLS-1$

        this.versionControlClient = versionControlClient;
        this.repository = repository;
        this.serverPath = serverPath;
    }

    public boolean getPreview()
    {
        return preview;
    }

    public void setPreview(boolean preview)
    {
        this.preview = preview;
    }

    public void setVersionSpec(VersionSpec versionSpec)
    {
        localVersionSpec = versionSpec;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor)
    {
        final String workspaceName =
            MessageFormat.format("{0}-{1}", GitTFConstants.GIT_TF_NAME, GUID.newGUID().getGUIDString()); //$NON-NLS-1$

        File tempFolder = null;
        Workspace tempWorkspace = null;
        boolean cleanup = false;

        progressMonitor.beginTask(Messages.getString("CreateWorkspaceTask.CreatingWorkspace"), //$NON-NLS-1$
            TaskProgressMonitor.INDETERMINATE,
            TaskProgressDisplay.DISPLAY_PROGRESS);

        try
        {
            if (!ServerPath.isServerPath(serverPath))
            {
                return new TaskStatus(TaskStatus.ERROR, Messages.formatString(
                    "CreateWorkspaceTask.TFSPathNotValidFormat", //$NON-NLS-1$
                    serverPath));
            }

            tempFolder = DirectoryUtil.getTempDir(repository);

            if (!tempFolder.mkdirs())
            {
                return new TaskStatus(TaskStatus.ERROR, Messages.formatString(
                    "CreateWorkspaceTask.CouldNotCreateTempDirFormat", //$NON-NLS-1$
                    workingFolder.getAbsolutePath()));
            }

            if (!preview)
            {
                tempWorkspace = versionControlClient.createWorkspace(new WorkingFolder[]
                {
                    new WorkingFolder(serverPath, tempFolder.getAbsolutePath())
                }, workspaceName, Messages.getString("CreateWorkspaceTask.WorkspaceComment"), //$NON-NLS-1$
                    WorkspaceLocation.SERVER,
                    WorkspaceOptions.NONE);

                if (updateLocalVersion)
                {
                    UpdateLocalVersionTask updateLocalVersionTask = null;
                    if (localVersionSpec != null)
                    {
                        updateLocalVersionTask =
                            new UpdateLocalVersionToSpecificVersionsTask(tempWorkspace, repository, localVersionSpec);
                    }
                    else
                    {
                        updateLocalVersionTask =
                            new UpdateLocalVersionToLatestBridgedChangesetTask(tempWorkspace, repository);
                    }

                    TaskStatus updateStatus =
                        new TaskExecutor(progressMonitor.newSubTask(TaskProgressMonitor.INDETERMINATE)).execute(updateLocalVersionTask);

                    if (!updateStatus.isOK())
                    {
                        cleanup = true;
                        return updateStatus;
                    }
                }

                this.workspace = new TfsWorkspace(tempWorkspace);
            }
            else
            {
                this.workspace = new PreviewOnlyWorkspace(progressMonitor);
            }

            this.workingFolder = tempFolder;

            progressMonitor.endTask();
        }
        catch (Exception e)
        {
            cleanup = true;
            return new TaskStatus(TaskStatus.ERROR, e);
        }
        finally
        {
            if (cleanup && tempFolder != null)
            {
                try
                {
                    FileHelpers.deleteDirectory(tempFolder);
                }
                catch (Exception e)
                {
                    log.warn(
                        MessageFormat.format("Could not clean up temporary folder {0}", tempFolder.getAbsolutePath()), //$NON-NLS-1$
                        e);
                }
            }

            if (cleanup && tempWorkspace != null)
            {
                try
                {
                    versionControlClient.deleteWorkspace(tempWorkspace);
                }
                catch (Exception e)
                {
                    log.warn(MessageFormat.format("Could not clean up temporary workspace {0}", workspaceName), e); //$NON-NLS-1$
                }
            }
        }

        return TaskStatus.OK_STATUS;
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
