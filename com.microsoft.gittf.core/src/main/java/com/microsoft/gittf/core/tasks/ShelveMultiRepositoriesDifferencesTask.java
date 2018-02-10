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

import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.config.ChangesetCommitMap;
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.interfaces.WorkspaceService;
import com.microsoft.gittf.core.tasks.framework.*;
import com.microsoft.gittf.core.tasks.pendDiff.PendDifferenceTask;
import com.microsoft.gittf.core.tasks.pendDiff.RenameMode;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.CommitWalker;
import com.microsoft.gittf.core.util.CommitWalker.CommitDelta;
import com.microsoft.gittf.core.util.RepositoryUtil;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pends the difference between the Commit Id specified and the latest bridged
 * commit to TFS, then shelves the differences in a shelveset on the TFS server.
 */
public class ShelveMultiRepositoriesDifferencesTask
        extends MultiRepositoriesWorkspaceTask {
    private static final Log log = LogFactory.getLog(ShelveMultiRepositoriesDifferencesTask.class);

    private final Map<File, ObjectId> gitDirWithShelveCommitID;
    private final String shelvesetName;

    private WorkItemCheckinInfo[] workItems;
    private boolean replace = false;
    private RenameMode renameMode = RenameMode.JUSTFILES;
    private String message = null;

    private VersionSpec shelveAgainstVersion = null;

    /**
     * Constructor
     *
     * @param repositories             the set of git repositories
     * @param gitDirWithShelveCommitID the map with key as working dir and value as commit id to shelve
     * @param versionControlClient     the version control client object
     * @param shelvesetName            the shelveset name
     */
    public ShelveMultiRepositoriesDifferencesTask(
            final Set<Repository> repositories,
            final Map<File, ObjectId> gitDirWithShelveCommitID,
            final VersionControlClient versionControlClient,
            final String shelvesetName) {
        super(repositories, versionControlClient);

        Check.notNullOrEmpty(gitDirWithShelveCommitID, "shelveCommitID");
        Check.notNullOrEmpty(shelvesetName, "shelvesetName");

        this.gitDirWithShelveCommitID = gitDirWithShelveCommitID;
        this.shelvesetName = shelvesetName;
    }

    /**
     * Sets the work item info for the work items to associate
     *
     * @param workItems
     */
    public void setWorkItemCheckinInfo(WorkItemCheckinInfo[] workItems) {
        this.workItems = workItems;
    }

    /**
     * Sets the flag that indicates the ability to replace an existing shelveset
     * with the same name on the server
     *
     * @param replace
     */
    public void setReplaceExistingShelveset(boolean replace) {
        this.replace = replace;
    }

    /**
     * Sets the rename mode to use when shelving the changes
     *
     * @param renameMode
     */
    public void setRenameMode(RenameMode renameMode) {
        this.renameMode = renameMode;
    }

    /**
     * Sets the shelveset comment
     *
     * @param message
     */
    public void setMessage(String message) {
        Check.notNullOrEmpty(message, "message");
        this.message = message;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor) {
        // TODO What about error message here and progress monitor at all?
        progressMonitor.beginTask(
                Messages.formatString(
                        "ShelveDifferenceTask.ShelvingDifferencesFormat", "Not implemented"), 1,
                TaskProgressDisplay.DISPLAY_PROGRESS.combine(TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL));

        WorkspaceInfo workspaceInfo = null;
        try {
            workspaceInfo = createWorkspace(progressMonitor.newSubTask(1), false, shelveAgainstVersion);
            final List<PendingChange[]> pendingChanges = new ArrayList<>();
            final WorkspaceService workspace = workspaceInfo.getWorkspace();
            for (final Repository repository : repositories) {
                final CommitDelta deltaToShelve = getOptimalCommitDelta(repository);
                final RevCommit fromCommit = deltaToShelve.getFromCommit();
                final RevCommit toCommit = deltaToShelve.getToCommit();
                final GitTFConfiguration config = GitTFConfiguration.loadFrom(repository);

                progressMonitor.setDetail(Messages.getString("ShelveDifferenceTask.PreparingWorkspace"));

                final File workingFolder = workspaceInfo.getRepoFolderToWorkingFolder().get(repository.getDirectory());

                final PendDifferenceTask pendTask =
                        new PendDifferenceTask(repository, fromCommit, toCommit, workspace, config.getServerPath(), workingFolder);
                pendTask.setRenameMode(renameMode);

                final TaskStatus pendStatus = new TaskExecutor(progressMonitor.newSubTask(1)).execute(pendTask);

                if (!pendStatus.isOK()) {
                    return pendStatus;
                }

                pendingChanges.add(pendTask.getPendingChanges());

                if (RepositoryUtil.hasUncommittedChanges(repository)) {
                    progressMonitor.displayWarning(Messages.getString("ShelveDifferenceTask.UnCommittedChangesDetected"));
                }

            }
            if (pendingChanges.isEmpty()) {
                throw new RuntimeException(Messages.getString("ShelveDifferenceTask.NoChangesToShelve"));
            }

            // TODO What about Streams here? -_-"
            List<PendingChange> changesToShelveList = new ArrayList<>();
            for (PendingChange[] change : pendingChanges) {
                if (change != null) {
                    changesToShelveList.addAll(Arrays.asList(change));
                }
            }

            /* Shelve the pended changes */
            final ShelvePendingChangesTask shelveTask =
                    new ShelvePendingChangesTask(
                            message,
                            workspace,
                            changesToShelveList.toArray(new PendingChange[changesToShelveList.size()]),
                            shelvesetName);

            shelveTask.setReplaceExistingShelveset(replace);
            shelveTask.setWorkItemCheckinInfo(workItems);

            progressMonitor.setDetail(Messages.getString("ShelveDifferenceTask.Shelving"));

            final TaskStatus shelveStatus = new TaskExecutor(progressMonitor.newSubTask(1)).execute(shelveTask);

            progressMonitor.setDetail(null);

            if (!shelveStatus.isOK()) {
                return shelveStatus;
            }

            /* Clean up the workspace */
            disposeWorkspace(progressMonitor.newSubTask(1));

            progressMonitor.endTask();

            return TaskStatus.OK_STATUS;
        } catch (Exception e) {
            log.error("Task exited with the following error", e);

            return new TaskStatus(TaskStatus.ERROR, e);
        } finally {
            if (workspaceInfo != null) {
                disposeWorkspace(new NullTaskProgressMonitor());
            }
        }
    }

    private Set<CommitDelta> getOptimalCommitDeltas() {
        return repositories.stream()
                .map(this::getOptimalCommitDelta)
                .collect(Collectors.toSet());
    }

    private CommitDelta getOptimalCommitDelta(final Repository r) {
        try {
            return getOptimalCommitDelta(r, gitDirWithShelveCommitID.get(r.getDirectory()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CommitDelta getOptimalCommitDelta(final Repository repository, final ObjectId shelveCommitID)
            throws Exception {
        final ChangesetCommitMap commitMap = new ChangesetCommitMap(repository);
        final int shelvesetChangesetId = commitMap.getChangesetID(shelveCommitID);

        if (shelvesetChangesetId > 0) {
            // this already maps to an existing changeset;
            throw new Exception(Messages.formatString("ShelveDifferenceTask.NoChangesToShelveFormat",
                    Integer.toString(shelvesetChangesetId)));
        }

        List<CommitDelta> commitDeltas = null;

        int currentChangesetId = commitMap.getLastBridgedChangesetID(true);
        while (currentChangesetId > 0) {
            ObjectId changesetCommitId = commitMap.getCommitID(currentChangesetId, true);

            try {
                commitDeltas = CommitWalker.getAutoSquashedCommitList(repository, changesetCommitId, shelveCommitID);
            } catch (Exception exception) {
                // eat exception here we do not care if the path does not exist
            }

            if (commitDeltas == null) {
                currentChangesetId = commitMap.getPreviousBridgedChangeset(currentChangesetId, true);
            } else {
                break;
            }
        }

        if (commitDeltas == null) {
            RevWalk walker = new RevWalk(repository);
            try {
                RevCommit toCommit = walker.parseCommit(shelveCommitID);
                return new CommitDelta(null, toCommit);
            } finally {
                if (walker != null) {
                    walker.release();
                }
            }
        }

        shelveAgainstVersion = new ChangesetVersionSpec(currentChangesetId);

        final RevCommit fromCommit = commitDeltas.get(0).getFromCommit();
        final RevCommit toCommit = commitDeltas.get(commitDeltas.size() - 1).getToCommit();

        return new CommitDelta(fromCommit, toCommit);
    }
}
