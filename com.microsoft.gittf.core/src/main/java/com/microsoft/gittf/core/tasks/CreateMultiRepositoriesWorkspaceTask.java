/*
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
 */

package com.microsoft.gittf.core.tasks;

import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.impl.PreviewOnlyWorkspace;
import com.microsoft.gittf.core.impl.TfsWorkspace;
import com.microsoft.gittf.core.interfaces.WorkspaceService;
import com.microsoft.gittf.core.tasks.framework.*;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

public class CreateMultiRepositoriesWorkspaceTask extends Task {
    private final static Log log = LogFactory.getLog(CreateMultiRepositoriesWorkspaceTask.class);

    private final VersionControlClient versionControlClient;
    private final Set<Repository> repositories;

    private boolean updateLocalVersion = true;
    private boolean preview = false;
    private VersionSpec localVersionSpec = null;

    private WorkspaceService workspace;
    private Set<File> workingFolders = new HashSet<>();

    public CreateMultiRepositoriesWorkspaceTask(
            final VersionControlClient versionControlClient,
            final Set<Repository> repositories) {
        // TODO Different error messages? (Different from original CreateWorkspaceTask)
        Check.notNull(versionControlClient, "versionControlClient");
        Check.notNullOrEmpty(repositories, "repository");

        this.versionControlClient = versionControlClient;
        this.repositories = repositories;
    }

    public boolean getPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public void setVersionSpec(VersionSpec versionSpec) {
        localVersionSpec = versionSpec;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor) {
        final String workspaceName =
                MessageFormat.format("{0}-{1}", GitTFConstants.GIT_TF_NAME, GUID.newGUID().getGUIDString());

        Workspace tempWorkspace = null;
        boolean cleanup = false;

        progressMonitor.beginTask(Messages.getString("CreateWorkspaceTask.CreatingWorkspace"),
                TaskProgressMonitor.INDETERMINATE,
                TaskProgressDisplay.DISPLAY_PROGRESS);

        final Set<File> tempFolders = new HashSet<>();
        try {
            final Set<WorkingFolder> workingFolders = new HashSet<>();
            for (Repository repository : repositories) {
                final GitTFConfiguration config = GitTFConfiguration.loadFrom(repository);
                final File tempFolder = DirectoryUtil.getTempDir(repository);
                tempFolders.add(tempFolder);
                final String serverPath = config.getServerPath();

                if (!ServerPath.isServerPath(serverPath)) {
                    return new TaskStatus(TaskStatus.ERROR, Messages.formatString(
                            "CreateWorkspaceTask.TFSPathNotValidFormat",
                            serverPath));
                }

                if (!tempFolder.mkdirs()) {
                    return new TaskStatus(TaskStatus.ERROR, Messages.formatString(
                            "CreateWorkspaceTask.CouldNotCreateTempDirFormat",
                            tempFolder.getAbsolutePath()));
                }

                workingFolders.add(new WorkingFolder(config.getServerPath(), tempFolder.getAbsolutePath()));
            }

            if (!preview) {
                tempWorkspace = versionControlClient.createWorkspace(
                        workingFolders.toArray(new WorkingFolder[workingFolders.size()]),
                        workspaceName, Messages.getString("CreateWorkspaceTask.WorkspaceComment"),
                        WorkspaceLocation.SERVER,
                        WorkspaceOptions.NONE);

                if (updateLocalVersion) {
                    // TODO Iterate repos again? -_-"
                    for (final Repository repository : repositories) {
                        final UpdateLocalVersionTask updateLocalVersionTask = getUpdateLocalVersionTask(tempWorkspace, repository);
                        final TaskStatus updateStatus =
                                new TaskExecutor(progressMonitor.newSubTask(TaskProgressMonitor.INDETERMINATE))
                                        .execute(updateLocalVersionTask);

                        if (!updateStatus.isOK()) {
                            cleanup = true;
                            return updateStatus;
                        }
                    }
                }

                this.workspace = new TfsWorkspace(tempWorkspace);
            } else {
                this.workspace = new PreviewOnlyWorkspace(progressMonitor);
            }

            this.workingFolders = tempFolders;
            progressMonitor.endTask();
        } catch (Exception e) {
            cleanup = true;
            return new TaskStatus(TaskStatus.ERROR, e);
        } finally {
            if (cleanup && !tempFolders.isEmpty()) {
                tempFolders.stream()
                        .filter(tf -> !FileHelpers.deleteDirectory(tf))
                        .forEach(tf ->
                                log.warn(MessageFormat.format("Could not clean up temporary folder {0}", tf.getAbsolutePath())));
            }

            if (cleanup && tempWorkspace != null) {
                try {
                    versionControlClient.deleteWorkspace(tempWorkspace);
                } catch (Exception e) {
                    log.warn(MessageFormat.format("Could not clean up temporary workspace {0}", workspaceName), e);
                }
            }
        }

        return TaskStatus.OK_STATUS;
    }

    public WorkspaceService getWorkspace() {
        return workspace;
    }

    public Set<File> getWorkingFolders() {
        return workingFolders;
    }

    private UpdateLocalVersionTask getUpdateLocalVersionTask(final Workspace workspace, final Repository repository) {
        return (localVersionSpec != null) ?
                    new UpdateLocalVersionToSpecificVersionsTask(workspace, repository, localVersionSpec) :
                    new UpdateLocalVersionToLatestBridgedChangesetTask(workspace, repository);
    }
}
