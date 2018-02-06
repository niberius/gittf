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
import com.microsoft.gittf.core.interfaces.VersionControlService;
import com.microsoft.gittf.core.tasks.framework.*;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.ObjectIdUtil;
import com.microsoft.gittf.core.util.RepositoryUtil;
import com.microsoft.gittf.core.util.TfsBranchUtil;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.*;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;

import java.net.URI;

public class CloneTask
        extends Task {
    private static final Log log = LogFactory.getLog(CloneTask.class);
    private final URI serverURI;
    private final VersionControlService vcClient;
    private final String tfsPath;
    private final Repository repository;
    private final WorkItemClient witClient;
    private boolean bare;
    private VersionSpec versionSpec = LatestVersionSpec.INSTANCE;
    private int depth = 1;
    private boolean tag = true;

    public CloneTask(
            final URI serverURI,
            final VersionControlService vcClient,
            final String tfsPath,
            final Repository repository) {
        this(serverURI, vcClient, tfsPath, repository, null);
    }

    public CloneTask(
            final URI serverURI,
            final VersionControlService vcClient,
            final String tfsPath,
            final Repository repository,
            final WorkItemClient witClient) {
        Check.notNull(serverURI, "serverURI");
        Check.notNull(vcClient, "vcClient");
        Check.notNullOrEmpty(tfsPath, "tfsPath");
        Check.notNull(repository, "repository");

        this.serverURI = serverURI;
        this.vcClient = vcClient;
        this.tfsPath = tfsPath;
        this.repository = repository;
        this.witClient = witClient;
    }

    public boolean isBare() {
        return bare;
    }

    public void setBare(final boolean bare) {
        this.bare = bare;
    }

    public VersionSpec getVersionSpec() {
        return versionSpec;
    }

    public void setVersionSpec(final VersionSpec versionSpec) {
        Check.notNull(versionSpec, "versionSpec");

        this.versionSpec = versionSpec;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(final int depth) {
        Check.isTrue(depth >= 1, "depth >= 1");

        this.depth = depth;
    }

    public boolean getTag() {
        return tag;
    }

    public void setTag(final boolean tag) {
        this.tag = tag;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor)
            throws Exception {
        final String taskName = Messages.formatString("CloneTask.CloningFormat",
                tfsPath,
                bare ? repository.getDirectory().getAbsolutePath() : repository.getWorkTree().getAbsolutePath());

        progressMonitor.beginTask(
                taskName,
                1,
                TaskProgressDisplay.DISPLAY_PROGRESS.combine(TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL));

        /*
         * Query the changesets.
         */

        /* See if this is an actual server path. */
        final Item item = vcClient.getItem(tfsPath, versionSpec, DeletedState.NON_DELETED, GetItemsOptions.NONE);
        Check.notNull(item, "item");

        if (item.getItemType() != ItemType.FOLDER) {
            return new TaskStatus(TaskStatus.ERROR, Messages.formatString("CloneTask.CannotCloneFileFormat", tfsPath));
        }

        /* Determine the latest changeset on the server. */
        final Changeset[] changesets =
                vcClient.queryHistory(
                        tfsPath,
                        versionSpec,
                        0,
                        RecursionType.FULL,
                        null,
                        new ChangesetVersionSpec(0),
                        versionSpec,
                        depth,
                        false,
                        false,
                        false,
                        false);

        /*
         * Create and configure the repository.
         */

        repository.create(bare);

        final ConfigureRepositoryTask configureTask = new ConfigureRepositoryTask(repository, serverURI, tfsPath);
        configureTask.setTag(tag);

        final TaskStatus configureStatus = new TaskExecutor(new NullTaskProgressMonitor()).execute(configureTask);

        if (!configureStatus.isOK()) {
            return configureStatus;
        }

        if (changesets.length > 0) {
            ObjectId lastCommitID = null;
            ObjectId lastTreeID = null;
            Item[] previousChangesetItems = null;

            /*
             * Download changesets.
             */
            final int numberOfChangesetToDownload = changesets.length;

            progressMonitor.setWork(numberOfChangesetToDownload);

            for (int i = numberOfChangesetToDownload; i > 0; i--) {
                CreateCommitForChangesetVersionSpecTask commitTask =
                        new CreateCommitForChangesetVersionSpecTask(
                                repository,
                                vcClient,
                                changesets[i - 1],
                                previousChangesetItems,
                                lastCommitID,
                                witClient);

                TaskStatus commitStatus = new TaskExecutor(progressMonitor.newSubTask(1)).execute(commitTask);

                if (!commitStatus.isOK()) {
                    return commitStatus;
                }

                lastCommitID = commitTask.getCommitID();
                lastTreeID = commitTask.getCommitTreeID();
                previousChangesetItems = commitTask.getCommittedItems();

                Check.notNull(lastCommitID, "lastCommitID");
                Check.notNull(lastTreeID, "lastTreeID");

                new ChangesetCommitMap(repository).setChangesetCommit(
                        changesets[i - 1].getChangesetID(),
                        commitTask.getCommitID());

                progressMonitor.displayVerbose(Messages.formatString("CloneTask.ClonedFormat",
                        Integer.toString(changesets[i - 1].getChangesetID()),
                        ObjectIdUtil.abbreviate(repository, lastCommitID)));
            }

            progressMonitor.setDetail(Messages.getString("CloneTask.Finalizing"));

            /* Update master head reference */
            RefUpdate ref = repository.updateRef(Constants.R_HEADS + Constants.MASTER);
            ref.setNewObjectId(lastCommitID);
            ref.update();

            /* Create tfs branch */
            TfsBranchUtil.create(repository, Constants.R_HEADS + Constants.MASTER);

            /*
             * Check out the cloned commit.
             */
            if (!bare) {
                DirCache dirCache = repository.lockDirCache();
                DirCacheCheckout checkout = new DirCacheCheckout(repository, dirCache, lastTreeID);
                checkout.checkout();
                dirCache.unlock();

                RepositoryUtil.fixFileAttributes(repository);
            }

            progressMonitor.endTask();

            final int finalChangesetID = changesets[0].getChangesetID();

            if (numberOfChangesetToDownload == 1) {
                progressMonitor.displayMessage(Messages.formatString("CloneTask.ClonedFormat",
                        Integer.toString(finalChangesetID),
                        ObjectIdUtil.abbreviate(repository, lastCommitID)));
            } else {
                progressMonitor.displayMessage(Messages.formatString("CloneTask.ClonedMultipleFormat",
                        changesets.length,
                        Integer.toString(finalChangesetID),
                        ObjectIdUtil.abbreviate(repository, lastCommitID)));
            }
        } else {
            // the folder exists on the server but is empty

            progressMonitor.displayMessage(Messages.getString("CloneTask.NothingToDownload"));

            progressMonitor.displayMessage(Messages.formatString("CloneTask.ClonedFolderEmptyFormat",
                    tfsPath));
        }

        log.info("Clone task completed.");

        return TaskStatus.OK_STATUS;
    }
}
