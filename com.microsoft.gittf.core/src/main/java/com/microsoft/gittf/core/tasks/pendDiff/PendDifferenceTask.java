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

package com.microsoft.gittf.core.tasks.pendDiff;

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;
import static org.eclipse.jgit.lib.Constants.OBJ_TREE;

import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.NameConflictTreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.interfaces.WorkspaceService;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskProgressDisplay;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.CommitUtil;
import com.microsoft.gittf.core.util.ObjectIdUtil;
import com.microsoft.gittf.core.util.WorkspaceOperationErrorListener;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.StringHelpers;

/**
 * The task converts the differences between two commits into a list of pending
 * changes and pends these changes in the workspace specified
 * 
 */
public class PendDifferenceTask
    extends Task
{
    /**
     * Error code - there was no differences between the two commits, there is
     * nothing to pend
     */
    public static final int NOTHING_TO_PEND = 1;

    private final int MAX_CHANGES_TO_SEND;

    private final static Log log = LogFactory.getLog(PendDifferenceTask.class);

    private final Repository repository;
    private final RevCommit commitFrom;
    private final RevCommit commitTo;
    private final WorkspaceService workspace;
    private final String serverPathRoot;
    private final File localWorkingFolder;

    private final GitTFConfiguration configuration;

    private RenameMode renameMode = RenameMode.JUSTFILES;

    private PendingChange[] pendingChanges;

    private boolean validated = false;

    /**
     * Constructor
     * 
     * @param repository
     *        the git repository
     * @param commitFrom
     *        the source commit to use when determining the changes needed to
     *        pend
     * @param commitTo
     *        the target commit to use when determining the changes needed to
     *        pend
     * @param workspace
     *        the workspace to pend changes in
     * @param serverPathRoot
     *        the server path root that is mapped
     * @param localWorkingFolder
     *        the local working folder to pend changes in
     */
    public PendDifferenceTask(
        final Repository repository,
        final RevCommit commitFrom,
        final RevCommit commitTo,
        final WorkspaceService workspace,
        final String serverPathRoot,
        final File localWorkingFolder)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        /* Commit from may be null to add all files */
        Check.notNull(commitTo, "commitTo"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNullOrEmpty(serverPathRoot, "serverPathRoot"); //$NON-NLS-1$
        Check.notNull(localWorkingFolder, "localWorkingFolder"); //$NON-NLS-1$
        Check.isTrue(
            localWorkingFolder.exists() && localWorkingFolder.isDirectory(),
            "localWorkingFolder.exists && localWorkingFolder.isDirectory"); //$NON-NLS-1$

        MAX_CHANGES_TO_SEND = getMaxChangesToSend();

        this.repository = repository;
        this.commitFrom = commitFrom;
        this.commitTo = commitTo;
        this.workspace = workspace;
        this.serverPathRoot = serverPathRoot;
        this.localWorkingFolder = localWorkingFolder;

        this.configuration = GitTFConfiguration.loadFrom(repository);
        Check.notNull(this.configuration, "configuration"); //$NON-NLS-1$
    }

    /**
     * Gets the list of pending changes that have been pended
     * 
     * @return
     */
    public PendingChange[] getPendingChanges()
    {
        return pendingChanges;
    }

    /**
     * Sets the rename mode to use when determining the changes that needs to be
     * pended
     * 
     * @param renameMode
     */
    public void setRenameMode(RenameMode renameMode)
    {
        this.renameMode = renameMode;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor)
    {
        log.debug("Pend differences task started."); //$NON-NLS-1$

        progressMonitor.beginTask(
            Messages.formatString(
                "PendDifferencesTask.PendingChangesFormat", ObjectIdUtil.abbreviate(repository, commitTo.getId())), //$NON-NLS-1$
            100,
            TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL);

        /* Nothing to do if we're not going anywhere */
        if (commitFrom != null && commitFrom.getId().equals(commitTo.getId()))
        {
            return TaskStatus.OK_STATUS;
        }

        progressMonitor.setDetail(Messages.getString("PendDifferencesTask.ExaminingServerState")); //$NON-NLS-1$

        /*
         * Sanity check: we do not expect any pending changes in the workspace
         */
        PendingSet existingChanges = workspace.getPendingChanges(new String[]
        {
            ServerPath.combine(serverPathRoot, "*") //$NON-NLS-1$
            },
            RecursionType.FULL,
            false);

        if (existingChanges != null && existingChanges.getPendingChanges().length > 0)
        {
            log.info("Workspace has existing pending changes - was a previous check-in aborted?"); //$NON-NLS-1$

            workspace.undo(new ItemSpec[]
            {
                new ItemSpec(ServerPath.combine(serverPathRoot, "*"), RecursionType.FULL) //$NON-NLS-1$
            });
        }

        progressMonitor.worked(5);
        progressMonitor.setDetail(null);

        /* Get the RevTree objects for the to and from commits */
        RevTree fromTree = (commitFrom != null) ? commitFrom.getTree() : null;
        RevTree toTree = commitTo.getTree();
        Check.notNull(toTree, "toTree"); //$NON-NLS-1$

        final TaskProgressMonitor analyzeMonitor = progressMonitor.newSubTask(75);
        final CheckinAnalysisChangeCollection analysis;

        try
        {
            log.debug("Validate the commit tree objects for any violations"); //$NON-NLS-1$

            /* Validate the commit tree objects for any violations */
            validate();

            /*
             * If we are comparing two commits analyze the difference between
             * both commits
             */
            if (fromTree != null)
            {
                log.debug("Analyzing differences"); //$NON-NLS-1$

                analysis = analyzeDifferences(repository, fromTree, toTree, renameMode, analyzeMonitor);
            }
            /*
             * Otherwise we need to create ADD changes for all the items in the
             * tree
             */
            else
            {
                log.debug("Analyzing entire tree to pend ADDs"); //$NON-NLS-1$

                analysis = analyzeTree(repository, toTree, analyzeMonitor);
            }
        }
        catch (Exception e)
        {
            log.debug("Pend differences task error:", e); //$NON-NLS-1$

            return new TaskStatus(TaskStatus.ERROR, e);
        }

        analyzeMonitor.endTask();

        final TaskProgressMonitor pendMonitor = progressMonitor.newSubTask(20);

        /* If the analysis is empty then there is nothing to pend */
        if (analysis.isEmpty())
        {
            log.debug("Nothing to pend."); //$NON-NLS-1$

            return new TaskStatus(TaskStatus.OK, NOTHING_TO_PEND);
        }

        try
        {
            /* Pend the changes found in the analysis in the workspace */
            pendingChanges = pendChanges(analysis, pendMonitor);
        }
        catch (Exception e)
        {
            log.debug("Pend differences task error:", e); //$NON-NLS-1$
            log.debug("Undoing pended changes."); //$NON-NLS-1$

            workspace.undo(new ItemSpec[]
            {
                new ItemSpec(ServerPath.ROOT, RecursionType.FULL)
            });

            return new TaskStatus(TaskStatus.ERROR, e);
        }
        pendMonitor.endTask();

        progressMonitor.endTask();

        log.debug("Pend differences task ended."); //$NON-NLS-1$

        return TaskStatus.OK_STATUS;
    }

    /**
     * Runs all the validations required on the source and destination commits
     * 
     * @throws Exception
     */
    public void validate()
        throws Exception
    {
        if (!validated)
        {
            validateTree(commitTo);
        }

        validated = true;
    }

    /**
     * Validates the trees
     * 
     * @param commit
     *        the commit tree to validate
     * 
     * @throws Exception
     */
    private void validateTree(RevCommit commit)
        throws Exception
    {
        /* Validates TFS case sensitivity requirements */
        validateCaseSensitivityRequirements(commit);
    }

    /**
     * Validates that the commit tree does not have the any item twice with the
     * same name only different in case. TFS does not support having the same
     * item with different case so we should not allow that.
     * 
     * @param commit
     * @throws Exception
     */
    private void validateCaseSensitivityRequirements(RevCommit commit)
        throws Exception
    {
        /* Create a tree walker */
        RevTree tree = commit.getTree();
        final TreeWalk treeWalker = new NameConflictTreeWalk(repository);

        try
        {
            treeWalker.addTree(tree);
            treeWalker.setFilter(TreeFilter.ALL);

            /*
             * Build a list of items in the tree in a hash set for quicker
             * lookup
             */
            Set<String> existingTreeItems = new HashSet<String>();

            /* Walk the tree looking for duplicates in the tree */
            while (treeWalker.next())
            {
                String pathString = treeWalker.getPathString().toLowerCase();

                if (existingTreeItems.contains(pathString))
                {
                    throw new Exception(
                        Messages.formatString(
                            "PendDifferenceTask.SimilarItemWithDifferentCaseInCommitFormat", pathString, ObjectIdUtil.abbreviate(repository, commit.getId()))); //$NON-NLS-1$
                }

                existingTreeItems.add(pathString);

                int objectType = treeWalker.getFileMode(0).getObjectType();
                if (objectType == OBJ_TREE)
                {
                    treeWalker.enterSubtree();
                }
            }
        }
        finally
        {
            if (treeWalker != null)
            {
                treeWalker.release();
            }
        }
    }

    /**
     * Creates the CheckinAnalysisChangeCollection analysis object that includes
     * the list of pending changes needed that map to the differences between
     * the fromCommit and toCommit
     * 
     * @param repository
     *        the git repository
     * @param fromRootTree
     *        the from commit tree
     * @param toRootTree
     *        the to commit tree
     * @param renameMode
     *        the rename mode to use when generating the analysis
     * @param progressMonitor
     *        the progress monitor to use to report progress
     * @return
     * @throws Exception
     */
    public static CheckinAnalysisChangeCollection analyzeDifferences(
        Repository repository,
        RevObject fromRootTree,
        RevObject toRootTree,
        RenameMode renameMode,
        final TaskProgressMonitor progressMonitor)
        throws Exception
    {
        Check.notNull(fromRootTree, "fromRootTree"); //$NON-NLS-1$
        Check.notNull(toRootTree, "toRootTree"); //$NON-NLS-1$
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        progressMonitor.beginTask(
            Messages.getString("PendDifferencesTask.AnalyzingCommits"), TaskProgressMonitor.INDETERMINATE, TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL); //$NON-NLS-1$

        /* Init the analysis object */
        final CheckinAnalysisChangeCollection analysis =
            new CheckinAnalysisChangeCollection(repository, fromRootTree, toRootTree);

        log.debug("Walking thru the git-repository tree."); //$NON-NLS-1$

        /* Init the tree walker object */
        final TreeWalk treeWalker = new NameConflictTreeWalk(repository);
        final RenameDetector repositoryRenameDetector = new RenameDetector(repository);

        try
        {
            treeWalker.setRecursive(true);

            treeWalker.addTree(fromRootTree);
            treeWalker.addTree(toRootTree);

            treeWalker.setFilter(TreeFilter.ANY_DIFF);

            List<DiffEntry> treeDifferences = DiffEntry.scan(treeWalker);

            log.debug("The number of differences found: " + treeDifferences.size()); //$NON-NLS-1$

            /*
             * If we need to detect file renames then use the rename detector to
             * analayze the differences first
             */
            if (renameMode != RenameMode.NONE)
            {
                repositoryRenameDetector.addAll(treeDifferences);
                treeDifferences = repositoryRenameDetector.compute();
            }

            /*
             * If the rename mode is either none or file only then we do not
             * need to detect folder deletes as well, since deleting empty
             * folders that have items that have been renamed out is not
             * supported in TFS
             */
            if (renameMode != RenameMode.ALL)
            {
                analysis.setProcessDeletedFolders(false);
            }

            Map<String, DiffEntry> deleteChanges = new HashMap<String, DiffEntry>();
            Map<String, DiffEntry> addChanges = new HashMap<String, DiffEntry>();

            for (DiffEntry change : treeDifferences)
            {
                switch (change.getChangeType())
                {
                    case ADD:
                        addChanges.put(change.getNewPath().toUpperCase(), change);
                        break;

                    case DELETE:
                        deleteChanges.put(change.getOldPath().toUpperCase(), change);
                        break;
                }
            }

            /* Append each change in to the analysis object */
            for (DiffEntry change : treeDifferences)
            {
                switch (change.getChangeType())
                {
                    case ADD:
                    case COPY:
                        if (!isCaseSensitiveRename(change, deleteChanges, addChanges))
                        {
                            analysis.pendAdd(new AddChange(change.getNewPath(), CommitUtil.resolveAbbreviatedId(
                                repository,
                                change.getNewId())));
                            analysis.pendPropertyIfChanged(new PropertyChange(
                                change.getNewPath(),
                                CommitUtil.resolveAbbreviatedId(repository, change.getNewId()),
                                change.getNewMode()));
                        }
                        break;

                    case DELETE:
                        if (isCaseSensitiveRename(change, deleteChanges, addChanges))
                        {
                            DiffEntry addChange = addChanges.get(change.getOldPath().toUpperCase());

                            analysis.pendRename(new RenameChange(
                                change.getOldPath(),
                                addChange.getNewPath(),
                                CommitUtil.resolveAbbreviatedId(repository, addChange.getNewId()),
                                !change.getOldId().equals(addChange.getNewId())));
                            analysis.pendPropertyIfChanged(new PropertyChange(
                                addChange.getNewPath(),
                                CommitUtil.resolveAbbreviatedId(repository, addChange.getNewId()),
                                change.getOldMode(),
                                addChange.getNewMode()));
                        }
                        else
                        {
                            analysis.pendDelete(new DeleteChange(change.getOldPath(), change.getOldMode()));
                        }
                        break;

                    case MODIFY:
                        analysis.pendEdit(new EditChange(change.getNewPath(), CommitUtil.resolveAbbreviatedId(
                            repository,
                            change.getNewId())));
                        analysis.pendPropertyIfChanged(new PropertyChange(
                            change.getNewPath(),
                            CommitUtil.resolveAbbreviatedId(repository, change.getNewId()),
                            change.getOldMode(),
                            change.getNewMode()));
                        break;

                    case RENAME:
                        analysis.pendRename(new RenameChange(
                            change.getOldPath(),
                            change.getNewPath(),
                            CommitUtil.resolveAbbreviatedId(repository, change.getNewId()),
                            !change.getOldId().equals(change.getNewId())));
                        analysis.pendPropertyIfChanged(new PropertyChange(
                            change.getNewPath(),
                            CommitUtil.resolveAbbreviatedId(repository, change.getNewId()),
                            change.getOldMode(),
                            change.getNewMode()));
                }
            }
        }
        finally
        {
            if (treeWalker != null)
            {
                treeWalker.release();
            }

            progressMonitor.endTask();
        }

        log.debug("Walk thru the git-repository tree finished."); //$NON-NLS-1$

        return analysis;
    }

    private static boolean isCaseSensitiveRename(
        DiffEntry change,
        Map<String, DiffEntry> deleteChanges,
        Map<String, DiffEntry> addChanges)
    {
        switch (change.getChangeType())
        {
            case ADD:
                return deleteChanges.containsKey(change.getNewPath().toUpperCase());

            case DELETE:
                return addChanges.containsKey(change.getOldPath().toUpperCase());

            default:
                return false;
        }
    }

    /**
     * Creates a CheckinAnalysisChangeCollection for the to commit tree,
     * creating an ADD change for every item in the tree.
     * 
     * @param repository
     *        the git repository
     * @param toRootTree
     *        the to commit tree
     * @param progressMonitor
     *        the progress monitor to use to report progress
     * @return
     * @throws Exception
     */
    public static CheckinAnalysisChangeCollection analyzeTree(
        Repository repository,
        RevTree toRootTree,
        TaskProgressMonitor progressMonitor)
        throws Exception
    {
        Check.notNull(toRootTree, "toRootTree"); //$NON-NLS-1$
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        progressMonitor.beginTask(
            Messages.getString("PendDifferencesTask.AnalyzingCommits"), TaskProgressMonitor.INDETERMINATE, TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL); //$NON-NLS-1$

        /* Create the CheckinAnalysisChangeCollection object */
        final CheckinAnalysisChangeCollection analysis = new CheckinAnalysisChangeCollection();
        final TreeWalk treeWalker = new NameConflictTreeWalk(repository);

        log.debug("Walking thru the git-repository tree."); //$NON-NLS-1$

        try
        {
            treeWalker.setRecursive(true);
            treeWalker.addTree(toRootTree);
            treeWalker.setFilter(TreeFilter.ANY_DIFF);

            /* Walk the tree and pend and add for every item in the tree */
            while (treeWalker.next())
            {
                final ObjectId toID = treeWalker.getObjectId(0);

                if (!ObjectId.zeroId().equals(toID))
                {
                    analysis.pendAdd(new AddChange(treeWalker.getPathString(), toID));
                }
                else
                {
                    log.info(MessageFormat.format("Ignoring item {0} - type {1}", //$NON-NLS-1$
                        treeWalker.getPathString(),
                        (toRootTree != null ? treeWalker.getFileMode(1).getObjectType() : "none"))); //$NON-NLS-1$
                }
            }
        }
        finally
        {
            if (treeWalker != null)
            {
                treeWalker.release();
            }

            progressMonitor.endTask();
        }

        log.debug("Walk thru the git-repository tree finished."); //$NON-NLS-1$

        return analysis;
    }

    /**
     * Pend the changes in the CheckinAnalysisChangeCollection in the workspace
     * 
     * @param analysis
     *        the collection of changes to pend
     * @param progressMonitor
     *        the progress monitor to use to report progress
     * @return
     * @throws Exception
     */
    private PendingChange[] pendChanges(
        CheckinAnalysisChangeCollection analysis,
        final TaskProgressMonitor progressMonitor)
        throws Exception
    {
        Check.notNull(analysis, "analysis"); //$NON-NLS-1$

        progressMonitor.beginTask(
            Messages.getString("PendDifferencesTask.PendingChanges"), 6, TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL); //$NON-NLS-1$

        WorkspaceOperationErrorListener errorListener = null;

        try
        {
            errorListener = workspace.getErrorListener();

            /* Pend Renames */
            progressMonitor.setDetail(Messages.getString("PendDifferencesTask.PendingRenames")); //$NON-NLS-1$
            pendRenames(analysis, errorListener);
            progressMonitor.worked(1);

            /* Pend Deletes */
            progressMonitor.setDetail(Messages.getString("PendDifferencesTask.PendingDeletes")); //$NON-NLS-1$
            pendDeletes(analysis, errorListener);
            progressMonitor.worked(1);

            /* Pend Edits */
            progressMonitor.setDetail(Messages.getString("PendDifferencesTask.PendingEdits")); //$NON-NLS-1$
            pendEdits(analysis, errorListener);
            progressMonitor.worked(1);

            /* Pend Adds */
            progressMonitor.setDetail(Messages.getString("PendDifferencesTask.PendingAdds")); //$NON-NLS-1$
            pendAdds(analysis, errorListener);
            progressMonitor.worked(1);

            /* Pend Properties */
            progressMonitor.setDetail(Messages.getString("PendDifferencesTask.PendingProperties")); //$NON-NLS-1$
            pendProperties(analysis, errorListener);
            progressMonitor.worked(1);

            /*
             * Only get pending changes that are *beneath* the server path. We
             * have a lock on the server path itself, and we want to maintain
             * that.
             */
            progressMonitor.setDetail(Messages.getString("PendDifferencesTask.QueryingPendingChanges")); //$NON-NLS-1$
            PendingSet pendingSet = workspace.getPendingChanges(new String[]
            {
                ServerPath.combine(serverPathRoot, "*") //$NON-NLS-1$
                },
                RecursionType.FULL,
                false);
            progressMonitor.worked(1);

            return pendingSet != null ? pendingSet.getPendingChanges() : null;
        }
        finally
        {
            if (errorListener != null)
            {
                errorListener.dispose();
            }

            progressMonitor.endTask();
        }
    }

    /**
     * Pends the deletes in the CheckinAnalysisChangeCollection
     * 
     * @param analysis
     *        the collection of changes to pend
     * @param errorListener
     *        the error listener to use
     * @throws Exception
     */
    private void pendDeletes(
        final CheckinAnalysisChangeCollection analysis,
        final WorkspaceOperationErrorListener errorListener)
        throws Exception
    {
        final List<DeleteChange> deletesChunk = new ArrayList<DeleteChange>();

        for (final DeleteChange delete : analysis.getDeletes())
        {
            if (deletesChunk.size() == MAX_CHANGES_TO_SEND)
            {
                pendDeletesInt(deletesChunk, errorListener);
                deletesChunk.clear();
            }

            deletesChunk.add(delete);
        }

        pendDeletesInt(deletesChunk, errorListener);
    }

    private void pendDeletesInt(final List<DeleteChange> deletes, final WorkspaceOperationErrorListener errorListener)
        throws Exception
    {
        Check.notNull(deletes, "deletes"); //$NON-NLS-1$

        if (deletes.size() == 0)
        {
            return;
        }

        log.debug(MessageFormat.format("Sending DELETE changes for {0} files:", deletes.size())); //$NON-NLS-1$

        /* Build the delete specs */
        ItemSpec[] deleteSpecs = new ItemSpec[deletes.size()];
        for (int i = 0; i < deletes.size(); i++)
        {
            final DeleteChange delete = deletes.get(i);

            log.debug(MessageFormat.format("    {0}, mode={1}", delete.getPath(), delete.getType())); //$NON-NLS-1$

            deleteSpecs[i] =
                new ItemSpec(ServerPath.combine(serverPathRoot, delete.getPath()), delete.getType() == FileMode.TREE
                    ? RecursionType.FULL : RecursionType.NONE);
        }

        /* Pend the deletes in the workspace */
        int count =
            workspace.pendDelete(deleteSpecs, LockLevel.NONE, GetOptions.NO_DISK_UPDATE, PendChangesOptions.NONE);

        /*
         * Validate that there were no errors when pending the deletes and that
         * the count of the pending changes matches the count of expected
         * changes
         */
        errorListener.validate();

        if (count < deleteSpecs.length)
        {
            log.error(MessageFormat.format("Server added {0} DELETE changes instead of {1}", count, deleteSpecs.length)); //$NON-NLS-1$
            throw new Exception(Messages.getString("PendDifferencesTask.PendFailed")); //$NON-NLS-1$
        }
    }

    /**
     * Pends the edits in the CheckinAnalysisChangeCollection
     * 
     * @param analysis
     *        the collection of changes to pend
     * @param errorListener
     *        the error listener to use
     * @throws Exception
     */
    private void pendEdits(
        final CheckinAnalysisChangeCollection analysis,
        final WorkspaceOperationErrorListener errorListener)
        throws Exception
    {
        final List<EditChange> editsChunk = new ArrayList<EditChange>();

        for (final EditChange edit : analysis.getEdits())
        {
            if (editsChunk.size() == MAX_CHANGES_TO_SEND)
            {
                pendEditsInt(editsChunk, errorListener);
                editsChunk.clear();
            }

            editsChunk.add(edit);
        }

        pendEditsInt(editsChunk, errorListener);
    }

    private void pendEditsInt(final List<EditChange> edits, final WorkspaceOperationErrorListener errorListener)
        throws Exception
    {
        Check.notNull(edits, "edits"); //$NON-NLS-1$

        if (edits.size() == 0)
        {
            return;
        }

        log.debug(MessageFormat.format("Sending EDIT changes for {0} files:", edits.size())); //$NON-NLS-1$

        /* Builds the edit specs */
        final List<ItemSpec> editSpecs = new ArrayList<ItemSpec>();
        final List<LockLevel> lockLevels = new ArrayList<LockLevel>();

        for (final EditChange edit : edits)
        {
            log.debug(MessageFormat.format("    {0}", edit.getPath())); //$NON-NLS-1$

            extractToWorkingFolder(edit.getPath(), edit.getObjectID());

            editSpecs.add(new ItemSpec(ServerPath.combine(serverPathRoot, edit.getPath()), RecursionType.NONE));
            lockLevels.add(LockLevel.NONE);
        }

        Check.isTrue(editSpecs.size() == lockLevels.size(), "editSpecs.size == lockLevels.size"); //$NON-NLS-1$

        /* Pends the edits in the workspace */
        final int count =
            workspace.pendEdit(
                editSpecs.toArray(new ItemSpec[editSpecs.size()]),
                lockLevels.toArray(new LockLevel[lockLevels.size()]),
                null,
                GetOptions.NO_DISK_UPDATE,
                PendChangesOptions.NONE,
                null,
                true);

        /* Validate that the items have been pended correctly */
        errorListener.validate();

        if (count != editSpecs.size())
        {
            log.error(MessageFormat.format("Server added {0} EDIT changes instead of {1}", count, editSpecs.size())); //$NON-NLS-1$
            throw new Exception(Messages.getString("PendDifferencesTask.PendFailed")); //$NON-NLS-1$
        }
    }

    /**
     * Pends the adds in the CheckinAnalysisChangeCollection
     * 
     * @param analysis
     *        the collection of changes to pend
     * @param errorListener
     *        the error listener to use
     * @throws Exception
     */
    private void pendProperties(
        final CheckinAnalysisChangeCollection analysis,
        final WorkspaceOperationErrorListener errorListener)
        throws Exception
    {
        if (workspace.getServiceLevel().getValue() < WebServiceLevel.TFS_2012.getValue())
        {
            return;
        }

        final List<PropertyChange> propertiesChunk = new ArrayList<PropertyChange>();

        for (final PropertyChange property : analysis.getProperties())
        {
            if (propertiesChunk.size() == MAX_CHANGES_TO_SEND)
            {
                pendPropertiessInt(propertiesChunk, errorListener);
                propertiesChunk.clear();
            }

            propertiesChunk.add(property);
        }

        pendPropertiessInt(propertiesChunk, errorListener);
    }

    private void pendPropertiessInt(
        final List<PropertyChange> propertyChanges,
        final WorkspaceOperationErrorListener errorListener)
        throws Exception
    {
        Check.notNull(propertyChanges, "propertyChanges"); //$NON-NLS-1$

        int propertiesCount = propertyChanges.size();

        if (propertiesCount == 0)
        {
            return;
        }

        log.debug(MessageFormat.format("Sending PROPERTIES changes for {0} files:", propertiesCount)); //$NON-NLS-1$

        /* Build the adds item spec */
        for (int i = 0; i < propertiesCount; i++)
        {
            final PropertyChange propertyChange = propertyChanges.get(i);

            extractToWorkingFolder(propertyChange.getPath(), propertyChange.getObjectID());

            final String path = ServerPath.combine(serverPathRoot, propertyChange.getPath());

            final List<PropertyValue> properties = new ArrayList<PropertyValue>();

            if (propertyChange.isExecutablePropertyChanged())
            {
                log.debug(MessageFormat.format("    {0}, property=executable", path)); //$NON-NLS-1$
                properties.add(propertyChange.getExecutablePropertyValue());
            }

            /* Pend the property change in the workspace */
            final int count =
                workspace.pendPropertyChange(
                    path,
                    properties.toArray(new PropertyValue[properties.size()]),
                    RecursionType.NONE,
                    LockLevel.NONE);

            /* Validate that the adds have been pended correctly */
            errorListener.validate();

            if (count != 1)
            {
                log.error(MessageFormat.format("Server added {0} EDIT changes instead of {1}", count, propertiesCount)); //$NON-NLS-1$
                throw new Exception(Messages.getString("PendDifferencesTask.PendFailed")); //$NON-NLS-1$
            }
        }
    }

    private PropertyValue createBooleanProperty(final String key, final boolean value)
    {
        return new PropertyValue(key, (value ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Pends the renames in the CheckinAnalysisChangeCollection
     * 
     * @param analysis
     *        the collection of changes to pend
     * @param errorListener
     *        the error listener to use
     * @throws Exception
     */
    private void pendRenames(
        final CheckinAnalysisChangeCollection analysis,
        final WorkspaceOperationErrorListener errorListener)
        throws Exception
    {
        /*
         * If the rename mode was to detect all renames including folder renames
         * we need create a folder rename detector object to determine the
         * folder rename operations. Git does not track folder renames only file
         * renames, TFS does not allow renameing items and deleteing its parent
         * if it is empty and thus if we only pended renames on items TFS will
         * have stale folders.
         */
        if (renameMode == RenameMode.ALL)
        {
            /* Compute folder renames */
            final TfsFolderRenameDetector folderRenameDetector = analysis.createFolderRenameDetector();
            folderRenameDetector.compute();

            /*
             * Due to a tfs limitation we cannot pend a rename for a folder and
             * an item inside the folder in the same pendRename call, thus we
             * are batching the rename calls by the depth of the item path
             */
            for (final List<RenameChange> renames : folderRenameDetector.getRenameBatches())
            {
                pendBatchRenames(renames, errorListener);
            }
        }
        else
        {
            pendBatchRenames(analysis.getRenames(), errorListener);
        }
    }

    /**
     * Pends renames for the items specified in the list
     * 
     * @param renames
     *        the renames to pend
     * @param errorListener
     *        the error listener to report the errors through
     * @throws Exception
     */
    private void pendBatchRenames(final List<RenameChange> renames, final WorkspaceOperationErrorListener errorListener)
        throws Exception
    {
        final List<RenameChange> renamesChunk = new ArrayList<RenameChange>();

        for (final RenameChange rename : renames)
        {
            if (renamesChunk.size() == MAX_CHANGES_TO_SEND)
            {
                pendBatchRenamesInt(renamesChunk, errorListener);
                renamesChunk.clear();
            }

            renamesChunk.add(rename);
        }

        pendBatchRenamesInt(renamesChunk, errorListener);
    }

    private void pendBatchRenamesInt(
        final List<RenameChange> renames,
        final WorkspaceOperationErrorListener errorListener)
        throws Exception
    {
        Check.notNull(renames, "renames"); //$NON-NLS-1$
        Check.notNull(errorListener, "errorListener"); //$NON-NLS-1$

        if (renames.size() == 0)
        {
            return;
        }

        /* Build the item specs for renames */
        final List<String> renameOldPaths = new ArrayList<String>();
        final List<String> renameNewPaths = new ArrayList<String>();
        final List<Boolean> renameEdits = new ArrayList<Boolean>();
        final List<String> editRenameOldPaths = new ArrayList<String>();
        final List<String> editRenameNewPaths = new ArrayList<String>();

        /*
         * Build the item specs for rename edits since some of the items can be
         * renames and edits in the same time
         */
        final List<ItemSpec> editSpecs = new ArrayList<ItemSpec>();
        final List<LockLevel> lockLevels = new ArrayList<LockLevel>();

        for (int i = 0; i < renames.size(); i++)
        {
            final RenameChange rename = renames.get(i);

            if (rename.isEdit())
            {
                editRenameOldPaths.add(ServerPath.combine(serverPathRoot, rename.getOldPath()));
                editRenameNewPaths.add(ServerPath.combine(serverPathRoot, rename.getNewPath()));

                editSpecs.add(new ItemSpec(ServerPath.combine(serverPathRoot, rename.getNewPath()), RecursionType.NONE));
                lockLevels.add(LockLevel.NONE);
                renameEdits.add(true);
            }
            else
            {
                renameOldPaths.add(ServerPath.combine(serverPathRoot, rename.getOldPath()));
                renameNewPaths.add(ServerPath.combine(serverPathRoot, rename.getNewPath()));
            }
        }

        if (renameOldPaths.size() > 0)
        {
            log.debug(MessageFormat.format("Sending RENAME changes for {0} files:", renameOldPaths.size())); //$NON-NLS-1$

            for (int i = 0; i < renameOldPaths.size(); i++)
            {
                log.debug(MessageFormat.format("    {0} ==> {1}", renameOldPaths.get(i), renameNewPaths.get(i))); //$NON-NLS-1$
            }

            final int renamesCount =
                workspace.pendRename(
                    renameOldPaths.toArray(new String[renameOldPaths.size()]),
                    renameNewPaths.toArray(new String[renameNewPaths.size()]),
                    null,
                    LockLevel.NONE,
                    GetOptions.NO_DISK_UPDATE,
                    false,
                    PendChangesOptions.NONE);

            /* Validate that the renames were pended correctly */
            errorListener.validate();

            if (renamesCount < renameOldPaths.size())
            {
                log.error(MessageFormat.format(
                    "Server added {0} RENAME changes instead of {1}", renamesCount, renameOldPaths.size())); //$NON-NLS-1$
                throw new Exception(Messages.getString("PendDifferencesTask.PendFailed")); //$NON-NLS-1$
            }
        }

        /* Pend the edited renames */
        if (editRenameOldPaths.size() > 0)
        {
            log.debug(MessageFormat.format("Sending RENAME changes for {0} files:", editRenameOldPaths.size())); //$NON-NLS-1$

            for (int i = 0; i < renameOldPaths.size(); i++)
            {
                log.debug(MessageFormat.format("    {0} ==> {1}", editRenameOldPaths.get(i), editRenameNewPaths.get(i))); //$NON-NLS-1$
            }

            final int count =
                workspace.pendRename(
                    editRenameOldPaths.toArray(new String[editRenameOldPaths.size()]),
                    editRenameNewPaths.toArray(new String[editRenameNewPaths.size()]),
                    renameEdits.toArray(new Boolean[renameEdits.size()]),
                    LockLevel.NONE,
                    GetOptions.NO_DISK_UPDATE,
                    false,
                    PendChangesOptions.NONE);

            /* Validate that the renames were pended correctly */
            errorListener.validate();

            if (count < editRenameOldPaths.size())
            {
                log.error(MessageFormat.format(
                    "Server added {0} RENAME changes instead of {1}", count, editRenameOldPaths.size())); //$NON-NLS-1$
                throw new Exception(Messages.getString("PendDifferencesTask.PendFailed")); //$NON-NLS-1$
            }

            for (int i = 0; i < renames.size(); i++)
            {
                final RenameChange rename = renames.get(i);

                if (rename.isEdit())
                {
                    extractToWorkingFolder(rename.getNewPath(), rename.getObjectID());
                }
            }

            log.debug(MessageFormat.format("Sending EDIT changes for {0} files:", editSpecs.size())); //$NON-NLS-1$

            for (int i = 0; i < editSpecs.size(); i++)
            {
                log.debug(MessageFormat.format("    {0}", editSpecs.get(i).getItem())); //$NON-NLS-1$
            }

            final int editsCount =
                workspace.pendEdit(
                    editSpecs.toArray(new ItemSpec[editSpecs.size()]),
                    lockLevels.toArray(new LockLevel[lockLevels.size()]),
                    null,
                    GetOptions.NO_DISK_UPDATE,
                    PendChangesOptions.NONE,
                    null,
                    editSpecs.size() == 0);

            /* Validate that the edits were pended correctly */
            errorListener.validate();

            if (editsCount < editSpecs.size())
            {
                log.error(MessageFormat.format(
                    "Server added {0} EDIT changes instead of {1}", editsCount, editSpecs.size())); //$NON-NLS-1$
                throw new Exception(Messages.getString("PendDifferencesTask.PendFailed")); //$NON-NLS-1$
            }
        }
    }

    /**
     * Pends the adds in the CheckinAnalysisChangeCollection
     * 
     * @param analysis
     *        the collection of changes to pend
     * @param errorListener
     *        the error listener to use
     * @throws Exception
     */
    private void pendAdds(
        final CheckinAnalysisChangeCollection analysis,
        final WorkspaceOperationErrorListener errorListener)
        throws Exception
    {
        final List<AddChange> addsChunk = new ArrayList<AddChange>();

        for (final AddChange add : analysis.getAdds())
        {
            if (addsChunk.size() == MAX_CHANGES_TO_SEND)
            {
                pendAddsInt(addsChunk, errorListener);
                addsChunk.clear();
            }

            addsChunk.add(add);
        }

        pendAddsInt(addsChunk, errorListener);
    }

    private void pendAddsInt(final List<AddChange> adds, final WorkspaceOperationErrorListener errorListener)
        throws Exception
    {
        Check.notNull(adds, "adds"); //$NON-NLS-1$

        int addCount = adds.size();

        if (addCount == 0)
        {
            return;
        }

        log.debug(MessageFormat.format("Sending ADD changes for {0} files:", addCount)); //$NON-NLS-1$

        /* Build the adds item spec */
        final String[] addPaths = new String[addCount];
        for (int i = 0; i < addCount; i++)
        {
            final AddChange add = adds.get(i);

            extractToWorkingFolder(add.getPath(), add.getObjectID());

            addPaths[i] = ServerPath.combine(serverPathRoot, add.getPath());
            log.debug(MessageFormat.format("    {0}", addPaths[i])); //$NON-NLS-1$
        }

        /* Pend the adds in the workspace */
        final int count =
            workspace.pendAdd(addPaths, false, null, LockLevel.NONE, GetOptions.NO_DISK_UPDATE, PendChangesOptions.NONE);

        /* Validate that the adds have been pended correctly */
        errorListener.validate();

        if (count != addCount)
        {
            log.error(MessageFormat.format("Server added {0} ADD changes instead of {1}", count, addCount)); //$NON-NLS-1$
            throw new Exception(Messages.getString("PendDifferencesTask.PendFailed")); //$NON-NLS-1$
        }
    }

    /**
     * Extracts an item for the git repository to the path specified. This is
     * used to extract files whose content have changed and will need to be
     * uploaded to pend an edit for
     * 
     * @param itemPath
     *        the path on disk to extract the item to
     * @param objectID
     *        the object id of the blob to extract to the file
     * @throws Exception
     */
    private void extractToWorkingFolder(String itemPath, ObjectId objectID)
        throws Exception
    {
        /* Ensure that the location exits */
        File workingFile = new File(localWorkingFolder, itemPath);

        File parentDir = workingFile.getParentFile();
        boolean parentDirExist = parentDir.exists();
        if (!parentDirExist)
        {
            parentDirExist = parentDir.mkdirs();
        }

        if (!parentDirExist)
        {
            throw new Exception(Messages.formatString(
                "PendDifferenceTask.CouldNotCreateItemPathFormat", parentDir.getAbsolutePath())); //$NON-NLS-1$
        }

        if (workingFile.exists())
        {
            workingFile.delete();
        }

        /* Extract the item from git */
        FileOutputStream workingOutput = new FileOutputStream(workingFile);

        try
        {
            /* Copy the blob from the object database to the file */
            repository.getObjectDatabase().open(objectID, OBJ_BLOB).copyTo(workingOutput);

            if (!workingFile.exists())
            {
                throw new Exception(Messages.formatString("PendDifferenceTask.CouldNotCreateItemFormat", itemPath)); //$NON-NLS-1$
            }
        }
        finally
        {
            workingOutput.close();
        }
    }

    private static int getMaxChangesToSend()
    {
        final String MAX_CHANGES_TO_PEND_NAME = "GITTF_MAX_CHANGES"; //$NON-NLS-1$
        final String MAX_CHANGES_TO_PEND_NAME_ALTERNATE = "gittf_max_changes"; //$NON-NLS-1$
        final int DEFAULT_MAX_CHANGES_TO_PEND = 10;

        int count = -1;

        String value = PlatformMiscUtils.getInstance().getEnvironmentVariable(MAX_CHANGES_TO_PEND_NAME);

        if (StringHelpers.isNullOrEmpty(value))
        {
            value = PlatformMiscUtils.getInstance().getEnvironmentVariable(MAX_CHANGES_TO_PEND_NAME_ALTERNATE);
        }

        try
        {
            if (!StringHelpers.isNullOrEmpty(value))
            {
                count = Integer.parseInt(value);
            }

        }
        catch (final Exception e)
        {
        }

        return count > 0 ? count : DEFAULT_MAX_CHANGES_TO_PEND;
    }
}
