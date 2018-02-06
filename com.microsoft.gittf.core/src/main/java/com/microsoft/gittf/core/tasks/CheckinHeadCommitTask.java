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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.OutputConstants;
import com.microsoft.gittf.core.config.ChangesetCommitMap;
import com.microsoft.gittf.core.config.ChangesetCommitMapUtil;
import com.microsoft.gittf.core.config.ChangesetCommitMapUtil.ChangesetCommitDetails;
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.identity.TfsUserMap;
import com.microsoft.gittf.core.identity.UserMap;
import com.microsoft.gittf.core.interfaces.WorkspaceService;
import com.microsoft.gittf.core.tasks.framework.NullTaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskExecutor;
import com.microsoft.gittf.core.tasks.framework.TaskProgressDisplay;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.tasks.pendDiff.PendDifferenceTask;
import com.microsoft.gittf.core.tasks.pendDiff.RenameMode;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.CommitUtil;
import com.microsoft.gittf.core.util.CommitWalker;
import com.microsoft.gittf.core.util.CommitWalker.CommitDelta;
import com.microsoft.gittf.core.util.DateUtil;
import com.microsoft.gittf.core.util.ObjectIdUtil;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.util.FileHelpers;

/**
 * The CheckinHeadCommitTask checks in all the changes between HEAD in the
 * specified repository and the last downloaded/checked in changeset to TFS
 * 
 */
public class CheckinHeadCommitTask
    extends WorkspaceTask
{
    /**
     * Return code meaning that there was nothing to checkin
     */
    public static final int ALREADY_UP_TO_DATE = 1;

    private static final Log log = LogFactory.getLog(CheckinHeadCommitTask.class);

    private boolean deep = false;
    private boolean mentions = false;
    private AbbreviatedObjectId[] squashCommitIDs = new AbbreviatedObjectId[0];
    private WorkItemCheckinInfo[] workItems;
    private boolean lock = true;
    private boolean overrideGatedCheckin;
    private boolean autoSquashMultipleParents;
    private boolean preview = false;
    private String comment = null;
    private String buildDefinition = null;
    private boolean includeMetaDataInComment = false;
    private RenameMode renameMode = RenameMode.JUSTFILES;
    private boolean keepAuthor = false;
    private String userMapPath = GitTFConstants.GIT_TF_DEFAULT_USER_MAP;
    private final WorkItemClient witClient;

    /**
     * Constructor
     * 
     * @param repository
     *        the repository to checkin. The repository needs to be configured
     *        to work with Git tf
     * @param versionControlClient
     *        the version client object to use
     */
    public CheckinHeadCommitTask(
        final Repository repository,
        final VersionControlClient versionControlClient,
        final WorkItemClient witClient)
    {
        super(repository, versionControlClient, GitTFConfiguration.loadFrom(repository).getServerPath());
        this.witClient = witClient;
    }

    /**
     * Sets the deep option. The default is false.
     * 
     * @param deep
     */
    public void setDeep(final boolean deep)
    {
        this.deep = deep;
    }

    /**
     * Sets the mentions option. The default is false.
     * 
     * @param mentions
     */
    public void setMentions(final boolean mentions)
    {
        this.mentions = mentions;
    }

    /**
     * Sets the commit ids that should be squashed.
     * 
     * @param squashCommitIDs
     */
    public void setSquashCommitIDs(AbbreviatedObjectId[] squashCommitIDs)
    {
        this.squashCommitIDs = (squashCommitIDs == null) ? new AbbreviatedObjectId[0] : squashCommitIDs;
    }

    /**
     * Sets the work item checkin info to associate/resolve work items
     * 
     * @param workItems
     */
    public void setWorkItemCheckinInfo(WorkItemCheckinInfo[] workItems)
    {
        this.workItems = workItems;
    }

    /**
     * Sets whether gated check in build should be overriden or not
     * 
     * @param overrideGatedCheckin
     */
    public void setOverrideGatedCheckin(boolean overrideGatedCheckin)
    {
        this.overrideGatedCheckin = overrideGatedCheckin;
    }

    /**
     * Sets whether we should take a lock on the root folder or not when
     * checking in. This option is only used in deep checkin and is ignored in
     * shallow checkin. The default is true.
     * 
     * @param lock
     */
    public void setLock(boolean lock)
    {
        this.lock = lock;
    }

    /**
     * Sets whether the task should automatically figure out a path between the
     * HEAD commit and the last downloaded/checked in commit. This option is
     * ignored when checking in shallow mode. The default is false.
     * 
     * @param autoSquashMultipleParents
     */
    public void setAutoSquash(boolean autoSquashMultipleParents)
    {
        this.autoSquashMultipleParents = autoSquashMultipleParents;
    }

    /**
     * Sets whether the task should run only in preview mode
     * 
     * @param preview
     */
    public void setPreview(boolean preview)
    {
        this.preview = preview;
    }

    /**
     * Sets the checkin comment that should be used when creating the changeset
     * in TFS
     * 
     * @param comment
     */
    public void setComment(String comment)
    {
        this.comment = comment;
    }

    /**
     * Sets the build definition name that should be used if there are multiple
     * gated build definitions available.
     * 
     * @param buildDefinition
     */
    public void setBuildDefinition(String buildDefinition)
    {
        this.buildDefinition = buildDefinition;
    }

    /**
     * Sets whether the task should include meta data of the commit in the
     * checkin comment or not. The default is false.
     * 
     * @param includeMetaDataInComment
     */
    public void setIncludeMetaDataInComment(boolean includeMetaDataInComment)
    {
        this.includeMetaDataInComment = includeMetaDataInComment;
    }

    /**
     * Sets the rename mode to use when comparing the commits. The default is
     * ALL.
     * 
     * @param renameMode
     */
    public void setRenameMode(RenameMode renameMode)
    {
        this.renameMode = renameMode;
    }

    /**
     * Sets whether the task should use commit authors as changeset owners
     * 
     * @param keepAuthor
     */
    public void setKeepAuthor(final boolean keepAuthor)
    {
        this.keepAuthor = keepAuthor;
    }

    /**
     * Sets an absolute or relative path to the user map file
     * 
     * @param userMapPath
     */
    public void setUserMapPath(final String userMapPath)
    {
        this.userMapPath = userMapPath;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor)
    {
        progressMonitor.beginTask(Messages.formatString("CheckinHeadCommitTask.CheckingInToPathFormat", //$NON-NLS-1$
            preview ? Messages.getString("CheckinHeadCommitTask.Preview") : "", //$NON-NLS-1$ //$NON-NLS-2$
            serverPath), 1, TaskProgressDisplay.DISPLAY_PROGRESS.combine(TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL));

        WorkspaceInfo workspaceData = null;
        UserMap userMap = null;

        try
        {
            /* Create the temporary workspace */

            WorkspaceService workspace = null;
            File workingFolder = null;

            log.debug("Creating temporary workspace"); //$NON-NLS-1$

            workspaceData = createWorkspace(progressMonitor.newSubTask(1), preview);

            workspace = workspaceData.getWorkspace();
            workingFolder = workspaceData.getWorkingFolder();

            log.debug("Workspace " + workspace.getName() + " created for the folder " + workingFolder.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$

            int expectedChangesetNumber = -1;

            /* In deep mode we should always lock the workspace */
            if (lock && deep)
            {
                log.debug("Locking TFS resource"); //$NON-NLS-1$
                final TaskStatus lockStatus =
                    new TaskExecutor(progressMonitor.newSubTask(1)).execute(new LockTask(workspace, serverPath));

                if (!lockStatus.isOK())
                {
                    return lockStatus;
                }
            }
            /*
             * if we are not locking we should attempt to detect if other users
             * sneaked in a checkin while this checkin is being processed
             */
            else if (!deep)
            {
                log.debug("No lock requested. Checking the latest change set."); //$NON-NLS-1$

                Changeset[] latestChangesets =
                    versionControlClient.queryHistory(
                        ServerPath.ROOT,
                        LatestVersionSpec.INSTANCE,
                        0,
                        RecursionType.FULL,
                        null,
                        null,
                        null,
                        1,
                        false,
                        false,
                        false,
                        false);

                Check.notNull(latestChangesets, "latestChangesets"); //$NON-NLS-1$

                expectedChangesetNumber = latestChangesets[0].getChangesetID() + 1;

                log.debug("Expected change set number = " + expectedChangesetNumber); //$NON-NLS-1$
            }

            log.debug("Obtaining the HEAD commit in the master barnch."); //$NON-NLS-1$
            /* Get the HEAD commit id */
            final ObjectId headCommitID = CommitUtil.getMasterHeadCommitID(repository);

            /*
             * Retrieve the last bridged changeset and the latest changeset on
             * the server
             */
            log.debug("Loading change set/commit map"); //$NON-NLS-1$
            final ChangesetCommitMap commitMap = new ChangesetCommitMap(repository);
            final ChangesetCommitDetails lastBridgedChangeset =
                ChangesetCommitMapUtil.getLastBridgedChangeset(commitMap);
            final ChangesetCommitDetails latestChangeset =
                ChangesetCommitMapUtil.getLatestChangeset(commitMap, versionControlClient, serverPath);

            /*
             * This is a repository that has been configured and never checked
             * in to tfs before. We need to validate that the path in tfs either
             * does not exist or is empty
             */
            if (lastBridgedChangeset == null || lastBridgedChangeset.getChangesetID() < 0)
            {
                log.debug("Firts checking for the new repository. Check that the root folder is ampty or does notexist."); //$NON-NLS-1$
                Item[] items =
                    versionControlClient.getItems(
                        serverPath,
                        LatestVersionSpec.INSTANCE,
                        RecursionType.ONE_LEVEL,
                        DeletedState.NON_DELETED,
                        ItemType.ANY).getItems();

                if (items != null && items.length > 0)
                {
                    /* The folder can exist but has to be empty */
                    if (!(items.length == 1 && ServerPath.equals(items[0].getServerItem(), serverPath)))
                    {
                        return new TaskStatus(TaskStatus.ERROR, Messages.formatString(
                            "CheckinHeadCommitTask.CannotCheckinToANonEmptyFolderFormat", serverPath)); //$NON-NLS-1$
                    }
                }
            }

            /*
             * There is a changeset for this path on the server, but it does not
             * have a corresponding commit in the map. The user must merge with
             * the latest changeset.
             */
            else if (latestChangeset != null && latestChangeset.getCommitID() == null)
            {
                return new TaskStatus(TaskStatus.ERROR, Messages.formatString(
                    "CheckinHeadCommitTask.NotFastForwardFormat", //$NON-NLS-1$
                    Integer.toString(latestChangeset.getChangesetID())));
            }

            /*
             * The server path does not exist, but we have previously downloaded
             * some items from it, thus it has been deleted. We cannot proceed.
             */
            else if (latestChangeset == null && lastBridgedChangeset != null)
            {
                return new TaskStatus(TaskStatus.ERROR, Messages.formatString(
                    "CheckinHeadCommitTask.ServerPathDoesNotExistFormat", //$NON-NLS-1$
                    serverPath));
            }

            /*
             * The current HEAD is the latest changeset on the TFS server.
             * Nothing to do.
             */
            else if (latestChangeset != null && latestChangeset.getCommitID().equals(headCommitID))
            {
                return new TaskStatus(TaskStatus.OK, CheckinHeadCommitTask.ALREADY_UP_TO_DATE);
            }

            log.debug("Examining the repository"); //$NON-NLS-1$
            progressMonitor.setDetail(Messages.getString("CheckinHeadCommitTask.ExaminingRepository")); //$NON-NLS-1$

            log.debug("Building the list of commit sequence we need to checkin"); //$NON-NLS-1$

            /* Build the list of commit sequence we need to checkin */
            List<CommitDelta> commitsToCheckin =
                getCommitsToCheckin(latestChangeset != null ? latestChangeset.getCommitID() : null, headCommitID);

            progressMonitor.setDetail(null);

            int lastChangesetID = -1;
            ObjectId lastCommitID = null;

            boolean anyThingCheckedIn = false;
            boolean otherUserCheckinDetected = false;

            log.debug("Number of commits to checkin: " + commitsToCheckin.size()); //$NON-NLS-1$

            progressMonitor.setWork(commitsToCheckin.size() * 2);

            TaskStatus userMapErrorStatus = null;
            if (keepAuthor)
            {
                log.debug("Loading the user map."); //$NON-NLS-1$

                userMap = new TfsUserMap(versionControlClient.getConnection(), userMapPath, commitsToCheckin);

                progressMonitor.setDetail(Messages.getString("CheckinHeadCommitTask.MappingAuthors")); //$NON-NLS-1$

                userMap.load();
                userMap.check(progressMonitor);
                userMap.addGitUsers();
                userMap.searchTfsUsers(progressMonitor.newSubTask(1));

                if (userMap.isChanged() || !userMap.isOK())
                {
                    userMap.save();

                    final String userMapChangedMessage =
                        Messages.formatString("CheckinHeadCommitTask.UserMapChangedMessageFormat", //$NON-NLS-1$
                            userMap.getUserMapFile().getPath());
                    progressMonitor.displayMessage(userMapChangedMessage);

                    if (!userMap.isOK())
                    {
                        final String incompleteUserMapError =
                            Messages.formatString("CheckinHeadCommitTask.IncompleteUserMapFormat", //$NON-NLS-1$
                                userMap.getUserMapFile().getPath());

                        userMapErrorStatus = new TaskStatus(TaskStatus.ERROR, incompleteUserMapError);
                        progressMonitor.worked(0);

                        if (!preview)
                        {
                            return userMapErrorStatus;
                        }
                    }
                }

                log.debug("The user map is loaded."); //$NON-NLS-1$
                progressMonitor.setDetail(null);
            }

            log.debug("Processing commit deltas."); //$NON-NLS-1$

            /*
             * Loop the list of commit sequence and checkin the difference one
             * by one
             */
            for (int i = 0; i < commitsToCheckin.size(); i++)
            {
                CommitDelta commitDelta = commitsToCheckin.get(i);

                log.debug("Committing delta " //$NON-NLS-1$
                    + i
                    + ": from " //$NON-NLS-1$
                    + (commitDelta.getFromCommit() == null ? "initial commit" : commitDelta.getFromCommit().getName()) //$NON-NLS-1$
                    + " to " //$NON-NLS-1$
                    + commitDelta.getToCommit().getName());

                progressMonitor.setDetail(Messages.formatString("CheckinHeadCommitTask.CommitFormat", //$NON-NLS-1$
                    ObjectIdUtil.abbreviate(repository, commitDelta.getToCommit())));

                boolean isLastCommit = (i == (commitsToCheckin.size() - 1));

                /* Save space: clean working folder after each checkin */
                if (i > 0)
                {
                    log.debug("Cleaning the working folder" + workingFolder.getAbsolutePath()); //$NON-NLS-1$

                    cleanWorkingFolder(workingFolder);
                }

                /* Pend the differences between the two commits */
                final PendDifferenceTask pendTask =
                    new PendDifferenceTask(
                        repository,
                        commitDelta.getFromCommit(),
                        commitDelta.getToCommit(),
                        workspace,
                        serverPath,
                        workingFolder);

                pendTask.setRenameMode(renameMode);

                pendTask.validate();

                /* If this is preview mode, display the commit details HEADER */
                if (preview)
                {
                    if (i == 0)
                    {
                        progressMonitor.displayMessage(Messages.getString("CheckinHeadCommitTask.CheckedInPreview")); //$NON-NLS-1$
                        progressMonitor.displayMessage(""); //$NON-NLS-1$
                    }

                    ObjectId fromCommit = commitDelta.getFromCommit();
                    ObjectId toCommit = commitDelta.getToCommit();

                    if (fromCommit == null || fromCommit == ObjectId.zeroId() || commitsToCheckin.size() != 1)
                    {
                        progressMonitor.displayMessage(Messages.formatString(
                            "CheckinHeadCommitTask.CheckedInPreviewSingleCommitFormat", //$NON-NLS-1$
                            i + 1,
                            ObjectIdUtil.abbreviate(repository, toCommit)));
                    }
                    else
                    {
                        progressMonitor.displayMessage(Messages.formatString(
                            "CheckinHeadCommitTask.CheckedInPreviewDifferenceCommitsFormat", //$NON-NLS-1$
                            i + 1,
                            ObjectIdUtil.abbreviate(repository, toCommit),
                            ObjectIdUtil.abbreviate(repository, fromCommit)));
                    }

                    String checkinComment = comment == null ? buildCommitComment(commitDelta) : comment;

                    progressMonitor.displayMessage(""); //$NON-NLS-1$
                    progressMonitor.displayMessage(indentString(checkinComment));

                    progressMonitor.displayMessage(Messages.getString("CheckinHeadCommitTask.CheckedInPreviewTableHeader")); //$NON-NLS-1$
                    progressMonitor.displayMessage("---------------------------------------------------------------------"); //$NON-NLS-1$
                }

                log.debug("Pend the differences between the two commits."); //$NON-NLS-1$

                final TaskStatus pendStatus = new TaskExecutor(progressMonitor.newSubTask(1)).execute(pendTask);

                if (!pendStatus.isOK())
                {
                    return pendStatus;
                }

                if (pendStatus.getCode() == PendDifferenceTask.NOTHING_TO_PEND)
                {
                    continue;
                }

                anyThingCheckedIn = true;

                /* If this is preview mode, display the commit details FOOTER */
                if (preview)
                {
                    progressMonitor.displayMessage("---------------------------------------------------------------------"); //$NON-NLS-1$
                    progressMonitor.displayMessage(""); //$NON-NLS-1$
                }
                /* else perform the actual checkin */
                else
                {
                    log.debug("Checking in."); //$NON-NLS-1$

                    progressMonitor.setDetail(Messages.getString("CheckinHeadCommitTask.CheckingIn")); //$NON-NLS-1$

                    log.debug("Building the change set comment."); //$NON-NLS-1$
                    String commitComment = buildCommitComment(commitDelta);

                    /*
                     * Perform the checkin using the checkin pending changes
                     * task
                     */
                    final CheckinPendingChangesTask checkinTask =
                        new CheckinPendingChangesTask(repository, commitDelta.getToCommit(), comment == null
                            ? commitComment : comment, versionControlClient, workspace, pendTask.getPendingChanges());

                    checkinTask.setWorkItemCheckinInfo(getWorkItems(
                        progressMonitor.newSubTask(1),
                        commitComment,
                        isLastCommit));
                    checkinTask.setOverrideGatedCheckin(overrideGatedCheckin);
                    checkinTask.setBuildDefinition(buildDefinition);
                    checkinTask.setExpectedChangesetNumber(expectedChangesetNumber);
                    checkinTask.setUserMap(userMap);

                    log.debug("Staring the check-in task."); //$NON-NLS-1$

                    final TaskStatus checkinStatus =
                        new TaskExecutor(progressMonitor.newSubTask(1)).execute(checkinTask);

                    log.debug("The check-in task ended with the status code: " + checkinStatus.getCode()); //$NON-NLS-1$

                    if (!checkinStatus.isOK())
                    {
                        return checkinStatus;
                    }

                    lastChangesetID = checkinTask.getChangesetID();
                    lastCommitID = commitDelta.getToCommit();
                    otherUserCheckinDetected =
                        checkinStatus.getCode() == CheckinPendingChangesTask.CHANGESET_NUMBER_NOT_AS_EXPECTED;

                    log.debug("    lastChangesetID: " + lastChangesetID); //$NON-NLS-1$
                    log.debug("    lastCommitID: " + lastCommitID); //$NON-NLS-1$
                    log.debug("    otherUserCheckinDetected: " + otherUserCheckinDetected); //$NON-NLS-1$

                    expectedChangesetNumber = -1;

                    progressMonitor.displayVerbose(Messages.formatString(
                        "CheckinHeadCommitTask.CheckedInChangesetFormat", //$NON-NLS-1$
                        ObjectIdUtil.abbreviate(repository, lastCommitID),
                        Integer.toString(checkinTask.getChangesetID())));
                }
            }

            log.debug("Cleaning up the workspace."); //$NON-NLS-1$
            /* Clean up the workspace */
            final TaskProgressMonitor cleanupMonitor = progressMonitor.newSubTask(1);
            cleanupWorkspace(cleanupMonitor, workspaceData);
            workspaceData = null;

            if (userMapErrorStatus != null)
            {
                return userMapErrorStatus;
            }

            progressMonitor.endTask();

            /* Display checkin results for the user */
            if (!preview)
            {
                // There was nothing detected to checkin.
                if (!anyThingCheckedIn)
                {
                    return new TaskStatus(TaskStatus.OK, CheckinHeadCommitTask.ALREADY_UP_TO_DATE);
                }

                if (commitsToCheckin.size() == 1)
                {
                    progressMonitor.displayMessage(Messages.formatString(
                        "CheckinHeadCommitTask.CheckedInFormat", ObjectIdUtil.abbreviate(repository, lastCommitID), Integer.toString(lastChangesetID))); //$NON-NLS-1$
                }
                else
                {
                    progressMonitor.displayMessage(Messages.formatString(
                        "CheckinHeadCommitTask.CheckedInMultipleFormat", Integer.toString(commitsToCheckin.size()), Integer.toString(lastChangesetID))); //$NON-NLS-1$                
                }

                if (otherUserCheckinDetected)
                {
                    progressMonitor.displayWarning(Messages.getString("CheckinHeadCommitTask.OtherUserCheckinDetected")); //$NON-NLS-1$
                }
            }

            return TaskStatus.OK_STATUS;
        }
        catch (Exception e)
        {
            return new TaskStatus(TaskStatus.ERROR, e);
        }
        finally
        {
            if (workspaceData != null)
            {
                cleanupWorkspace(new NullTaskProgressMonitor(), workspaceData);
            }
        }
    }

    /**
     * Builds the commit comment to use when checking in
     * 
     * @param commitDelta
     * @return
     */
    private String buildCommitComment(CommitDelta commitDelta)
    {
        /* In deep mode the task will use the ToCommit message */
        if (deep)
        {
            /*
             * If the meta data flag was set to true, then build the meta data
             * string
             */
            if (includeMetaDataInComment)
            {
                return buildCommitComment(commitDelta.getToCommit());
            }
            /* Otherwise just use the full message */
            else
            {
                return commitDelta.getToCommit().getFullMessage();
            }
        }

        /*
         * In Shallow mode use the log command to identify all the commit
         * included in the delta
         */
        try
        {
            /*
             * TODO: Need to replace this code to use better logic to figure out
             * the included commits topologically and not chronologically
             */

            LogCommand logCommand = new Git(repository).log();
            logCommand.addRange(commitDelta.getFromCommit().getId(), commitDelta.getToCommit().getId());
            logCommand.setMaxCount(OutputConstants.DEFAULT_MAXCOMMENTROLLUP + 1);
            Iterable<RevCommit> commits = logCommand.call();

            int commitCounter = 0;

            StringBuilder comment = new StringBuilder();

            comment.append(Messages.formatString("CheckinHeadCommitTask.ShallowCheckinRollupFormat", //$NON-NLS-1$
                ObjectIdUtil.abbreviate(repository, commitDelta.getToCommit().getId()),
                ObjectIdUtil.abbreviate(repository, commitDelta.getFromCommit().getId())) + OutputConstants.NEW_LINE);
            comment.append(OutputConstants.NEW_LINE);

            for (RevCommit commit : commits)
            {
                commitCounter++;

                if (commitCounter > OutputConstants.DEFAULT_MAXCOMMENTROLLUP)
                {
                    comment.append(Messages.formatString(
                        "CheckinHeadCommitTask.ShallowCheckinCommentDisplayTruncatedFormat", //$NON-NLS-1$
                        OutputConstants.DEFAULT_MAXCOMMENTROLLUP,
                        ObjectIdUtil.abbreviate(repository, commit.getId()),
                        ObjectIdUtil.abbreviate(repository, commitDelta.getFromCommit().getId())));

                    break;
                }

                comment.append(buildCommitComment(commit) + OutputConstants.NEW_LINE);
            }

            if (commitCounter == 1)
            {
                return buildCommitComment(commitDelta.getToCommit());
            }

            return comment.toString();
        }
        catch (Exception e)
        {
            // if we fail execute the log command we default to the destination
            // commit full message

            return buildCommitComment(commitDelta.getToCommit());
        }
    }

    /**
     * Builds the comment for a single commit
     * 
     * @param commit
     * @return
     */
    private String buildCommitComment(RevCommit commit)
    {
        if (includeMetaDataInComment)
        {
            StringBuilder comment = new StringBuilder();

            comment.append(Messages.formatString("CheckinHeadCommitTask.ShallowCheckinCommentFormat", //$NON-NLS-1$
                ObjectIdUtil.abbreviate(repository, commit.getId()),
                DateUtil.formatDate(new Date(((long) commit.getCommitTime()) * 1000))) + OutputConstants.NEW_LINE);
            comment.append(Messages.formatString("CheckinHeadCommitTask.ShallowCheckinCommentAuthorFormat", //$NON-NLS-1$
                commit.getAuthorIdent().getName(),
                commit.getAuthorIdent().getEmailAddress()) + OutputConstants.NEW_LINE);
            comment.append(Messages.formatString("CheckinHeadCommitTask.ShallowCheckinCommentCommitterFormat", //$NON-NLS-1$
                commit.getCommitterIdent().getName(),
                commit.getCommitterIdent().getEmailAddress()) + OutputConstants.NEW_LINE);
            comment.append("-----------------------------------------------------------------" + OutputConstants.NEW_LINE); //$NON-NLS-1$
            comment.append(indentString(commit.getFullMessage()));
            comment.append(OutputConstants.NEW_LINE);

            return comment.toString();
        }
        else
        {
            return commit.getFullMessage();
        }
    }

    private String indentString(String input)
    {
        String[] lines = input.split(OutputConstants.NEW_LINE);

        StringBuilder sb = new StringBuilder();
        for (String line : lines)
        {
            sb.append(MessageFormat.format("    {0}{1}", line, OutputConstants.NEW_LINE)); //$NON-NLS-1$
        }

        return sb.toString();
    }

    /**
     * Gets the sequence of commit differences that need to be checked in
     * 
     * @param sourceCommitID
     * @param headCommitID
     * @return
     * @throws Exception
     */
    private List<CommitDelta> getCommitsToCheckin(final ObjectId sourceCommitID, final ObjectId headCommitID)
        throws Exception
    {
        log.debug("Detecting commit deltas."); //$NON-NLS-1$

        Check.notNull(headCommitID, "headCommitID"); //$NON-NLS-1$

        List<CommitDelta> commitsToCheckin;

        log.debug("Walking thru commit tree."); //$NON-NLS-1$
        /*
         * In the case of shallow commit, we do not care if the user provided
         * ids to squash or not since we are not preserving history anyways we
         * select any path we find and that would be ok
         */
        if (autoSquashMultipleParents || !deep)
        {
            commitsToCheckin = CommitWalker.getAutoSquashedCommitList(repository, sourceCommitID, headCommitID);
        }
        else
        {
            commitsToCheckin = CommitWalker.getCommitList(repository, sourceCommitID, headCommitID, squashCommitIDs);
        }

        int depth = deep ? Integer.MAX_VALUE : GitTFConstants.GIT_TF_SHALLOW_DEPTH;

        log.debug("Commit s to check-in number: " + commitsToCheckin.size()); //$NON-NLS-1$

        /* Prune the list of commits down to their depth. */
        if (commitsToCheckin.size() > depth)
        {
            log.debug("Prune commits to the depth: " + depth); //$NON-NLS-1$

            List<CommitDelta> prunedCommits = new ArrayList<CommitDelta>();

            RevCommit lastToCommit = null;
            RevCommit lastFromCommit = null;

            for (int i = 0; i < depth - 1; i++)
            {
                CommitDelta delta = commitsToCheckin.get(commitsToCheckin.size() - 1 - i);

                prunedCommits.add(delta);

                lastToCommit = delta.getFromCommit();
            }

            lastFromCommit = commitsToCheckin.get(0).getFromCommit();

            if (lastToCommit == null)
            {
                lastToCommit = commitsToCheckin.get(commitsToCheckin.size() - 1).getToCommit();
            }

            Check.notNull(lastToCommit, "lastToCommit"); //$NON-NLS-1$

            prunedCommits.add(new CommitDelta(lastFromCommit, lastToCommit));

            commitsToCheckin = prunedCommits;
        }

        log.debug("Detection commit deltas finished."); //$NON-NLS-1$

        return commitsToCheckin;
    }

    private void cleanWorkingFolder(final File workingFolder)
    {
        try
        {
            FileHelpers.deleteDirectory(workingFolder);
            workingFolder.mkdirs();
        }
        catch (Exception e)
        {
            /* Not fatal */
            log.warn(MessageFormat.format("Could not clean up temporary directory {0}", //$NON-NLS-1$
                workingFolder.getAbsolutePath()), e);
        }
    }

    private void cleanupWorkspace(final TaskProgressMonitor progressMonitor, final WorkspaceInfo workspaceData)
    {
        if (workspaceData == null)
        {
            return;
        }

        progressMonitor.beginTask(Messages.getString("CheckinHeadCommitTask.DeletingWorkspace"), //$NON-NLS-1$
            TaskProgressMonitor.INDETERMINATE,
            TaskProgressDisplay.DISPLAY_PROGRESS);

        final WorkspaceService workspace = workspaceData.getWorkspace();

        if (workspaceData.getWorkspace() != null && lock && deep)
        {
            final TaskStatus unlockStatus =
                new TaskExecutor(progressMonitor.newSubTask(1)).execute(new UnlockTask(workspace, serverPath));

            if (!unlockStatus.isOK())
            {
                log.warn(MessageFormat.format("Could not unlock {0}: {1}", serverPath, unlockStatus.getMessage())); //$NON-NLS-1$                
            }
        }

        disposeWorkspace(progressMonitor.newSubTask(1));

        progressMonitor.endTask();
    }

    /**
     * Parses the comments of the commits to list the mentioned work items
     */
    private WorkItemCheckinInfo[] getWorkItems(
        final TaskProgressMonitor progressMonitor,
        final String commitComment,
        final boolean isLastCommit)
        throws Exception
    {

        List<WorkItemCheckinInfo> workItemsCheckinInfo = new ArrayList<WorkItemCheckinInfo>();
        if (mentions)
        {
            final String REGEX = "(\\s|^)#\\d+(\\s|$)(#\\d+(\\s|$))*"; //$NON-NLS-1$
            if (commitComment != null && commitComment.length() > 0)
            {
                final Pattern pattern = Pattern.compile(REGEX);
                // get a matcher object
                final Matcher patternMatcher = pattern.matcher(commitComment);
                while (patternMatcher.find())
                {

                    final String workItemIDREGEX = "#\\d+"; //$NON-NLS-1$
                    final Pattern workItemIDPattern = Pattern.compile(workItemIDREGEX);
                    final String workItemIDString =
                        commitComment.substring(patternMatcher.start(), patternMatcher.end());
                    final Matcher workItemIDMatcher = workItemIDPattern.matcher(workItemIDString);
                    while (workItemIDMatcher.find())
                    {
                        final WorkItem workitem =
                            getWorkItem(
                                progressMonitor,
                                workItemIDString.substring(workItemIDMatcher.start(), workItemIDMatcher.end()));
                        if (workitem != null)
                        {
                            final WorkItemCheckinInfo workItemCheckinInfo =
                                new WorkItemCheckinInfo(workitem, CheckinWorkItemAction.ASSOCIATE);
                            if (!workItemsCheckinInfo.contains(workItemCheckinInfo))
                            {
                                workItemsCheckinInfo.add(workItemCheckinInfo);
                            }
                        }
                    }

                }
            }
        }
        if (isLastCommit)
        {
            // If there were no work items in the comments
            if (workItemsCheckinInfo.isEmpty())
            {
                return workItems;
            }

            for (final WorkItemCheckinInfo workItem : workItems)
            {
                if (!workItemsCheckinInfo.contains(workItem))
                {
                    workItemsCheckinInfo.add(workItem);
                }
            }
        }
        return workItemsCheckinInfo.toArray(new WorkItemCheckinInfo[workItemsCheckinInfo.size()]);
    }

    private WorkItem getWorkItem(final TaskProgressMonitor progressMonitor, final String mentionsString)
        throws Exception
    {
        final int id;

        try
        {
            id = Integer.parseInt(mentionsString.replace("#", "")); //$NON-NLS-1$//$NON-NLS-2$

            if (id <= 0)
            {
                progressMonitor.displayWarning(Messages.formatString(
                    "CheckinHeadCommitTask.WorkItemInvalidFormat", mentionsString)); //$NON-NLS-1$
                return null;
            }
        }
        catch (final NumberFormatException e)
        {
            progressMonitor.displayWarning(Messages.formatString(
                "CheckinHeadCommitTask.WorkItemInvalidFormat", mentionsString)); //$NON-NLS-1$
            return null;
        }

        final WorkItem workItem = witClient.getWorkItemByID(id);

        if (workItem == null)
        {
            progressMonitor.displayWarning(Messages.formatString("CheckinHeadCommitTask.WorkItemDoesNotExistFormat", id)); //$NON-NLS-1$
        }

        return workItem;
    }
}
