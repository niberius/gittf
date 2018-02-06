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

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;

import com.microsoft.gittf.core.Messages;
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
import com.microsoft.gittf.core.util.VersionSpecUtil;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;

public class PullTask
    extends Task
{
    private static final Log log = LogFactory.getLog(FetchTask.class);

    private final Repository repository;
    private final VersionControlService versionControlClient;
    private final WorkItemClient witClient;

    private VersionSpec versionSpec = LatestVersionSpec.INSTANCE;
    private boolean deep = false;
    private boolean rebase = false;
    private boolean force = false;

    private ObjectId fetchedCommitId;

    private MergeCommand mergeCommand;
    private RebaseCommand rebaseCommand;

    public PullTask(final Repository repository, final VersionControlService versionControlClient)
    {
        this(repository, versionControlClient, null);
    }

    public PullTask(
        final Repository repository,
        final VersionControlService versionControlClient,
        WorkItemClient witClient)
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

    public void setRebase(final boolean rebase)
    {
        this.rebase = rebase;
    }

    public void setStrategy(MergeStrategy mergeStrategy)
    {
        getMergeCommand().setStrategy(mergeStrategy);
    }

    public void setForce(final boolean force)
    {
        this.force = force;
    }

    public ObjectId getCommitId()
    {
        return fetchedCommitId;
    }

    private MergeCommand getMergeCommand()
    {
        if (mergeCommand == null)
        {
            mergeCommand = new Git(repository).merge();
        }

        return mergeCommand;
    }

    private RebaseCommand getRebaseCommand()
    {
        if (rebaseCommand == null)
        {
            rebaseCommand = new Git(repository).rebase();
        }

        return rebaseCommand;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor)
    {
        progressMonitor.beginTask(
            Messages.formatString(
                "PullTask.FetchingVersionFormat", GitTFConfiguration.loadFrom(repository).getServerPath(), VersionSpecUtil.getDescription(versionSpec)), 1, //$NON-NLS-1$
            TaskProgressDisplay.DISPLAY_PROGRESS.combine(TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL));

        final FetchTask fetchTask = new FetchTask(repository, versionControlClient, witClient);
        fetchTask.setVersionSpec(versionSpec);
        fetchTask.setDeep(deep);
        fetchTask.setForce(force);
        fetchTask.setShouldUpdateFetchHead(false);

        final TaskStatus fetchStatus = new TaskExecutor(progressMonitor.newSubTask(1)).execute(fetchTask);

        if (!fetchStatus.isOK())
        {
            return fetchStatus;
        }

        fetchedCommitId = fetchTask.getCommitId();

        if (rebase)
        {
            return rebase(progressMonitor, fetchTask.getCommitId());
        }
        else
        {
            return merge(progressMonitor, fetchTask.getLatestChangeSetId(), fetchTask.getCommitId());
        }
    }

    private TaskStatus merge(TaskProgressMonitor progressMonitor, int changeset, ObjectId commitId)
    {
        try
        {
            getMergeCommand().include(
                Messages.formatString("PullTask.Merge.CommitNameFormat", Integer.toString(changeset)), commitId); //$NON-NLS-1$

            MergeResult mergeResults = getMergeCommand().call();

            progressMonitor.endTask();

            switch (mergeResults.getMergeStatus())
            {
                case ALREADY_UP_TO_DATE:
                    progressMonitor.displayMessage(Messages.getString("PullTask.Merge.AlreadyUpToDate")); //$NON-NLS-1$
                    break;

                case FAST_FORWARD:
                case MERGED:
                    progressMonitor.displayMessage(Messages.formatString(
                        "PullTask.Merge.MergeSuccessfulFormat", ObjectIdUtil.abbreviate(repository, commitId))); //$NON-NLS-1$
                    break;

                case CONFLICTING:
                    progressMonitor.displayMessage(Messages.formatString(
                        "PullTask.Merge.MergeSuccessfulWithConflictsFormat", ObjectIdUtil.abbreviate(repository, commitId))); //$NON-NLS-1$
                    displayConflicts(progressMonitor, mergeResults.getConflicts());
                    break;

                case FAILED:
                case NOT_SUPPORTED:
                    progressMonitor.displayMessage(Messages.formatString(
                        "PullTask.Merge.FailedFormat", ObjectIdUtil.abbreviate(repository, commitId))); //$NON-NLS-1$
                    displayFailures(progressMonitor, mergeResults.getFailingPaths());
                    break;
            }

            RepositoryUtil.fixFileAttributes(repository);
        }
        catch (Exception e)
        {
            log.error("An error occurred while merging.", e); //$NON-NLS-1$

            return new TaskStatus(TaskStatus.ERROR, e);
        }

        return TaskStatus.OK_STATUS;
    }

    private TaskStatus rebase(TaskProgressMonitor progressMonitor, ObjectId commitId)
    {
        try
        {
            getRebaseCommand().setOperation(Operation.BEGIN);
            getRebaseCommand().setUpstream(commitId);
            getRebaseCommand().setProgressMonitor(new RebaseMergeJGITProgressMonitor(progressMonitor));

            RebaseResult rebaseResults = getRebaseCommand().call();

            progressMonitor.endTask();

            RebaseResult.Status status = rebaseResults.getStatus();

            switch (status)
            {
                case UP_TO_DATE:
                    progressMonitor.displayMessage(Messages.getString("PullTask.Rebase.AlreadyUpToDate")); //$NON-NLS-1$
                    break;

                case ABORTED:
                case FAILED:
                    progressMonitor.displayMessage(Messages.formatString(
                        "PullTask.Rebase.FailedFormat", ObjectIdUtil.abbreviate(repository, commitId))); //$NON-NLS-1$
                    displayFailures(progressMonitor, rebaseResults.getFailingPaths());
                    break;

                case FAST_FORWARD:
                case OK:
                    progressMonitor.displayMessage(Messages.formatString(
                        "PullTask.Rebase.RebaseSuccessfulFormat", ObjectIdUtil.abbreviate(repository, commitId))); //$NON-NLS-1$
                    break;

                case NOTHING_TO_COMMIT:
                    progressMonitor.displayMessage(Messages.formatString(
                        "PullTask.Rebase.NothingToCommitFormat", ObjectIdUtil.abbreviate(repository, commitId))); //$NON-NLS-1$
                    break;

                case STOPPED:
                    progressMonitor.displayMessage(Messages.formatString(
                        "PullTask.Rebase.StoppedFormat", ObjectIdUtil.abbreviate(repository, commitId))); //$NON-NLS-1$
                    break;
            }

            RepositoryUtil.fixFileAttributes(repository);
        }
        catch (Exception e)
        {
            log.error("An error occurred while merging.", e); //$NON-NLS-1$

            return new TaskStatus(TaskStatus.ERROR, e);
        }

        return TaskStatus.OK_STATUS;
    }

    private void displayConflicts(TaskProgressMonitor progressMonitor, Map<String, int[][]> conflicts)
    {
        if (conflicts != null)
        {
            for (Entry<String, int[][]> conflict : conflicts.entrySet())
            {
                progressMonitor.displayMessage(conflict.getKey());
            }
        }
    }

    private void displayFailures(TaskProgressMonitor progressMonitor, Map<String, MergeFailureReason> failures)
    {
        if (failures != null)
        {
            for (Entry<String, MergeFailureReason> failure : failures.entrySet())
            {
                progressMonitor.displayMessage(Messages.formatString(
                    "PullTask.Merge.FailedMergeEntryFormat", failure.getKey(), getFailureMessage(failure.getValue()))); //$NON-NLS-1$
            }
        }
    }

    private String getFailureMessage(MergeFailureReason value)
    {
        switch (value)
        {
            case COULD_NOT_DELETE:
                return Messages.getString("PullTask.Merge.CouldNotDeleteFile"); //$NON-NLS-1$

            case DIRTY_INDEX:
                return Messages.getString("PullTask.Merge.DirtyHeadIndex"); //$NON-NLS-1$

            case DIRTY_WORKTREE:
                return Messages.getString("PullTask.Merge.DirtyWorkTree"); //$NON-NLS-1$
        }

        return Messages.getString("PullTask.Merge.UnKnownError"); //$NON-NLS-1$
    }

    private class RebaseMergeJGITProgressMonitor
        implements ProgressMonitor
    {
        private final TaskProgressMonitor progressMonitor;

        public RebaseMergeJGITProgressMonitor(TaskProgressMonitor progressMonitor)
        {
            this.progressMonitor = progressMonitor;
        }

        public void start(int totalTasks)
        {
        }

        public void beginTask(String title, int totalWork)
        {
            progressMonitor.displayMessage(Messages.formatString("Pull.RebaseMergeJGITProgressMonitorFormat", title)); //$NON-NLS-1$
        }

        public void update(int completed)
        {
        }

        public void endTask()
        {
        }

        public boolean isCancelled()
        {
            return false;
        }
    }
}
