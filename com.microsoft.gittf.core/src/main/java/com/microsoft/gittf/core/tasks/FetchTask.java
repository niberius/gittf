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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.internal.storage.file.LockFile;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.config.ChangesetCommitMap;
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.interfaces.VersionControlService;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskExecutor;
import com.microsoft.gittf.core.tasks.framework.TaskProgressDisplay;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.ObjectIdUtil;
import com.microsoft.gittf.core.util.RepositoryUtil;
import com.microsoft.gittf.core.util.TfsBranchUtil;
import com.microsoft.gittf.core.util.VersionSpecUtil;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;

public class FetchTask
    extends Task
{
    public static final int CHANGESET_ALREADY_FETCHED = 1;

    private static final Log log = LogFactory.getLog(FetchTask.class);

    private final Repository repository;
    private final VersionControlService versionControlClient;
    private final WorkItemClient witClient;

    private VersionSpec versionSpec = LatestVersionSpec.INSTANCE;
    private boolean deep = false;
    private boolean shouldUpdateFetchHead = true;
    private boolean force = false;

    private ObjectId fetchedCommitId = null;
    private int fetchedChangesetId = -1;

    public FetchTask(final Repository repository, final VersionControlService versionControlClient)
    {
        this(repository, versionControlClient, null);
    }

    public FetchTask(
        final Repository repository,
        final VersionControlService versionControlClient,
        final WorkItemClient witClient)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(versionControlClient, "versionControlClient"); //$NON-NLS-1$

        this.repository = repository;
        this.versionControlClient = versionControlClient;
        this.witClient = witClient;
    }

    public void setVersionSpec(VersionSpec versionSpecToFetch)
    {
        versionSpec = versionSpecToFetch;
    }

    public void setDeep(final boolean deep)
    {
        this.deep = deep;
    }

    public void setShouldUpdateFetchHead(boolean shouldUpdateFetchHead)
    {
        this.shouldUpdateFetchHead = shouldUpdateFetchHead;
    }

    public void setForce(final boolean force)
    {
        this.force = force;
    }

    public ObjectId getCommitId()
    {
        return fetchedCommitId;
    }

    public int getLatestChangeSetId()
    {
        return fetchedChangesetId;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor)
    {
        boolean alreadyFetched = false;

        progressMonitor.beginTask(
            Messages.formatString(
                "FetchTask.FetchingVersionFormat", GitTFConfiguration.loadFrom(repository).getServerPath(), VersionSpecUtil.getDescription(versionSpec)), 1, //$NON-NLS-1$
            TaskProgressDisplay.DISPLAY_PROGRESS.combine(TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL));

        final GitTFConfiguration configuration = GitTFConfiguration.loadFrom(repository);
        final ChangesetCommitMap changesetCommitMap = new ChangesetCommitMap(repository);

        int latestChangesetID = changesetCommitMap.getLastBridgedChangesetID(true);

        /*
         * If nothing has been fetched or checked in by Git-TF before, i.e. this
         * is a configured repo, we need to check if the Git repository itself
         * is not empty. In that case we have to show a message and exit.
         * Otherwise we may continue because that is the safe first fetch into
         * empty just configured repository.
         */
        if (latestChangesetID < 0)
        {
            try
            {
                if (RepositoryUtil.isEmptyRepository(repository))
                {
                    log.info("This is a newly configured empty repository. Continue fetching"); //$NON-NLS-1$
                    latestChangesetID = 0;
                }
                else
                {
                    return new TaskStatus(
                        TaskStatus.ERROR,
                        Messages.getString("FetchTask.NothingToFetchInNewlyConfiguredRepo")); //$NON-NLS-1$
                }
            }
            catch (IOException e)
            {
                return new TaskStatus(TaskStatus.ERROR, e);
            }
        }

        Changeset[] latestChangesets =
            versionControlClient.queryHistory(
                configuration.getServerPath(),
                versionSpec,
                0,
                RecursionType.FULL,
                null,
                new ChangesetVersionSpec(force && latestChangesetID > 0 ? latestChangesetID - 1 : latestChangesetID),
                versionSpec,
                deep || force ? Integer.MAX_VALUE : GitTFConstants.GIT_TF_SHALLOW_DEPTH,
                false,
                false,
                false,
                false);

        if (latestChangesets.length == 0 && force)
        {
            latestChangesets =
                versionControlClient.queryHistory(
                    configuration.getServerPath(),
                    versionSpec,
                    0,
                    RecursionType.FULL,
                    null,
                    null,
                    versionSpec,
                    deep || force ? Integer.MAX_VALUE : GitTFConstants.GIT_TF_SHALLOW_DEPTH,
                    false,
                    false,
                    false,
                    false);
        }

        if (latestChangesets.length == 0)
        {
            return new TaskStatus(TaskStatus.ERROR, Messages.formatString(
                "FetchTask.CouldNotDetermineVersionFormat", versionSpec.toString(), configuration.getServerPath())); //$NON-NLS-1$
        }

        int finalChangesetID = latestChangesets[0].getChangesetID();
        ObjectId finalCommitID = changesetCommitMap.getCommitID(finalChangesetID, true);

        int changesetCounter = 0;

        if (finalCommitID != null && !force)
        {
            log.info(MessageFormat.format("The changeset to download {0} has been downloaded before in commit id {1}", //$NON-NLS-1$
                Integer.toString(finalChangesetID),
                finalCommitID.toString()));

            fetchedChangesetId = finalChangesetID;

            alreadyFetched = true;
        }
        else
        {
            log.info(MessageFormat.format(
                "Downloading changeset {0} and creating a new commit", Integer.toString(finalChangesetID))); //$NON-NLS-1$

            ObjectId lastCommitID =
                (latestChangesetID >= 0) ? changesetCommitMap.getCommitID(latestChangesetID, true) : null;

            /*
             * Note: since we query history from last bridged changeset ->
             * latest, we may have gotten the last bridged changeset returned to
             * us as the last element. (This will be true if depth > number of
             * changesets since last bridged changeset.) Filter this changeset
             * out.
             */
            Changeset[] changesets = calculateChangesetsToDownload(latestChangesets, latestChangesetID);

            changesetCounter = changesets.length - 1;
            Item[] previousChangesetItems =
                versionControlClient.getItems(
                    configuration.getServerPath(),
                    new ChangesetVersionSpec(latestChangesetID),
                    RecursionType.FULL);

            progressMonitor.setWork(changesetCounter + 1);

            for (int i = changesetCounter; i >= 0; i--)
            {
                progressMonitor.setDetail(Messages.formatString("FetchTask.ChangesetNumberFormat", //$NON-NLS-1$
                    Integer.toString(changesets[i].getChangesetID())));

                CreateCommitForChangesetVersionSpecTask createCommitTask =
                    new CreateCommitForChangesetVersionSpecTask(
                        repository,
                        versionControlClient,
                        changesets[i],
                        previousChangesetItems,
                        lastCommitID,
                        witClient);
                TaskStatus createCommitTaskStatus =
                    new TaskExecutor(progressMonitor.newSubTask(1)).execute(createCommitTask);

                if (!createCommitTaskStatus.isOK())
                {
                    log.info("Commit Creation failed"); //$NON-NLS-1$

                    return createCommitTaskStatus;
                }

                lastCommitID = createCommitTask.getCommitID();
                fetchedChangesetId = changesets[i].getChangesetID();
                previousChangesetItems = createCommitTask.getCommittedItems();

                try
                {
                    boolean forceHWMUpdate = i == changesetCounter && force;
                    changesetCommitMap.setChangesetCommit(changesets[i].getChangesetID(), lastCommitID, forceHWMUpdate);
                }
                catch (IOException e)
                {
                    return new TaskStatus(TaskStatus.ERROR, e);
                }

                progressMonitor.displayVerbose(Messages.formatString("FetchTask.FetchedChangesetFormat", //$NON-NLS-1$
                    Integer.toString(changesets[i].getChangesetID()),
                    ObjectIdUtil.abbreviate(repository, lastCommitID)));
            }

            finalCommitID = lastCommitID;
        }

        fetchedCommitId = finalCommitID;

        progressMonitor.endTask();

        /* update fetch head */

        if (shouldUpdateFetchHead)
        {
            boolean updatedFetchHead = false;

            try
            {
                updatedFetchHead = writeFetchHead(finalCommitID, finalChangesetID);
            }
            catch (IOException e)
            {
                return new TaskStatus(TaskStatus.ERROR, e);
            }

            if (alreadyFetched)
            {
                if (updatedFetchHead)
                {
                    progressMonitor.displayMessage(Messages.formatString(
                        "FetchTask.AlreadyFetchedUpdateFetchHeadFormat", //$NON-NLS-1$
                        Integer.toString(finalChangesetID),
                        ObjectIdUtil.abbreviate(repository, finalCommitID)));
                }
                else
                {
                    progressMonitor.displayMessage(Messages.getString("FetchTask.AlreadyFetchedNothingToUpdate")); //$NON-NLS-1$
                }
            }
            else
            {
                if (changesetCounter <= 1)
                {
                    progressMonitor.displayMessage(Messages.formatString("FetchTask.FetchedFormat", //$NON-NLS-1$
                        Integer.toString(finalChangesetID),
                        ObjectIdUtil.abbreviate(repository, finalCommitID)));
                }
                else
                {
                    progressMonitor.displayMessage(Messages.formatString("FetchTask.FetchedMultipleFormat", //$NON-NLS-1$
                        changesetCounter,
                        Integer.toString(finalChangesetID),
                        ObjectIdUtil.abbreviate(repository, finalCommitID)));
                }
            }
        }

        try
        {
            TfsBranchUtil.update(repository, finalCommitID);
        }
        catch (Exception e)
        {
            return new TaskStatus(TaskStatus.ERROR, e);
        }

        log.info("Fetch task complete"); //$NON-NLS-1$

        return TaskStatus.OK_STATUS;
    }

    private Changeset[] calculateChangesetsToDownload(Changeset[] changesets, int latestChangeset)
    {
        Check.notNullOrEmpty(changesets, "changesets"); //$NON-NLS-1$

        List<Changeset> changesetsToDownload = new ArrayList<Changeset>(changesets.length);
        changesetsToDownload.add(changesets[0]);

        for (int count = 1; count < changesets.length; count++)
        {
            if ((changesets[count].getChangesetID() > latestChangeset && deep)
                || (changesets[count].getChangesetID() == latestChangeset && force))
            {
                changesetsToDownload.add(changesets[count]);
            }

            if (changesets[count].getChangesetID() < latestChangeset)
            {
                break;
            }
        }

        return changesetsToDownload.toArray(new Changeset[changesetsToDownload.size()]);
    }

    private boolean writeFetchHead(final ObjectId commitID, final int changesetID)
        throws IOException
    {
        Ref fetchHeadRef = repository.getRef(Constants.FETCH_HEAD);
        boolean referencesEqual = fetchHeadRef == null ? false : fetchHeadRef.getObjectId().equals(commitID);

        if (referencesEqual)
        {
            return false;
        }

        final File refFile = new File(repository.getDirectory(), Constants.FETCH_HEAD);
        final LockFile lockFile = new LockFile(refFile, repository.getFS());

        if (lockFile.lock())
        {
            try
            {
                BufferedWriter writer =
                    new BufferedWriter(new OutputStreamWriter(lockFile.getOutputStream(), Charset.forName("UTF-8"))); //$NON-NLS-1$

                try
                {
                    writer.append(MessageFormat.format("{0}\t\t{1}", commitID.getName(), //$NON-NLS-1$
                        Messages.formatString("FetchTask.RefLogFormat", //$NON-NLS-1$
                            Integer.toString(changesetID))));
                }
                finally
                {
                    writer.close();
                }

                lockFile.commit();
            }
            finally
            {
                lockFile.unlock();
            }
        }

        return true;
    }
}
