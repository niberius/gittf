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

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.config.ChangesetCommitMap;
import com.microsoft.gittf.core.interfaces.VersionControlService;
import com.microsoft.gittf.core.tasks.framework.TaskProgressDisplay;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.tree.CommitTreeEntry;
import com.microsoft.gittf.core.util.tree.CommitTreePath;
import com.microsoft.gittf.core.util.tree.CommitTreePathComparator;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.artifact.ArtifactIDFactory;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.util.FileHelpers;

public class CreateCommitForChangesetVersionSpecTask
    extends CreateCommitTask
{
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    private static final String HASH = "#"; //$NON-NLS-1$
    private static final String SPACES = "          "; //$NON-NLS-1$
    private static final int WIT_TITLE_PAD_WIDTH = SPACES.length();

    private static final Log log = LogFactory.getLog(CreateCommitForChangesetVersionSpecTask.class);

    private final int changesetID;
    private final Changeset changeset;
    private ObjectId commitTreeID;
    private final WorkItemClient witClient;
    private Item[] committedItems;
    private final Item[] previousChangesetItems;

    public CreateCommitForChangesetVersionSpecTask(
        final Repository repository,
        final VersionControlService versionControlClient,
        final Changeset changeset,
        final Item[] previousCommittedItems,
        final ObjectId parentCommitID,
        final WorkItemClient witClient)
    {
        super(repository, versionControlClient, parentCommitID);

        Check.isTrue(changeset.getChangesetID() >= 0, "changesetID >= 0"); //$NON-NLS-1$

        this.changesetID = changeset.getChangesetID();
        this.witClient = witClient;
        this.changeset = changeset;
        this.previousChangesetItems = previousCommittedItems;
    }

    public ObjectId getCommitTreeID()
    {
        return commitTreeID;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor)
    {
        progressMonitor.beginTask(Messages.formatString("CreateCommitForChangesetVersionSpecTask.CreatingCommitFormat", //$NON-NLS-1$
            Integer.toString(changesetID)), 1, TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL);

        ObjectInserter repositoryInserter = null;

        try
        {
            validateTempDirectory();

            /* Make sure that the changeset requested exist on the server */
            if (changeset == null)
            {
                throw new Exception(Messages.formatString(
                    "CreateCommitForChangesetVersionSpecTask.ChangesetNotFoundFormat", //$NON-NLS-1$
                    Integer.toString(changesetID)));
            }

            /*
             * Retrieve the items at the specified changeset version from the
             * server
             */

            committedItems =
                versionControlService.getItems(serverPath, new ChangesetVersionSpec(changesetID), RecursionType.FULL);

            /*
             * We want to optimize the tree building process. To do so we will
             * inspect the changeset commit map for the previous changeset
             * downloaded and use it to extract the objectIds for the files that
             * have not changed.
             */

            final ChangesetCommitMap changesetCommitMap = new ChangesetCommitMap(repository);
            final int previousChangesetId = changesetCommitMap.getPreviousBridgedChangeset(changesetID, true);
            final ObjectId previousChangesetCommitId =
                previousChangesetId >= 0 ? changesetCommitMap.getCommitID(previousChangesetId, true) : null;

            final ChangesetCommitItemReader previousChangesetCommitReader =
                new ChangesetCommitItemReader(previousChangesetId, previousChangesetCommitId, previousChangesetItems);

            /*
             * We want trees sorted by children first so we can simply walk them
             * (child-first) to build the hierarchy once we've finished
             * inserting blobs.
             */
            final Map<CommitTreePath, Map<CommitTreePath, CommitTreeEntry>> treeHierarchy =
                new TreeMap<CommitTreePath, Map<CommitTreePath, CommitTreeEntry>>(new CommitTreePathComparator());

            repositoryInserter = repository.newObjectInserter();

            /*
             * Phase one: insert files as blobs in the git repository and add
             * them to the TreeFormatter for their parent folder.
             */
            if (committedItems != null)
            {
                progressMonitor.setWork(committedItems.length);
                for (final Item item : committedItems)
                {
                    createBlob(repositoryInserter, treeHierarchy, previousChangesetCommitReader, item, progressMonitor);

                    progressMonitor.worked(1);
                }
            }

            /* Phase two: add child trees to their parents. */
            progressMonitor.setDetail(Messages.getString("CreateCommitTask.CreatingTrees")); //$NON-NLS-1$
            final ObjectId rootTree = createTrees(repositoryInserter, treeHierarchy);

            /* Phase three: create the commit. */
            progressMonitor.setDetail(Messages.getString("CreateCommitTask.CreatingCommit")); //$NON-NLS-1$            
            final ObjectId commit = createCommit(repositoryInserter, rootTree, changeset);

            repositoryInserter.flush();

            FileHelpers.deleteDirectory(tempDir);

            progressMonitor.endTask();

            this.commitId = commit;
            this.commitTreeID = rootTree;

            return TaskStatus.OK_STATUS;
        }
        catch (Exception e)
        {
            log.error(e);
            return new TaskStatus(TaskStatus.ERROR, e);
        }
        finally
        {
            if (repositoryInserter != null)
            {
                repositoryInserter.release();
            }
        }
    }

    private void createBlob(
        final ObjectInserter repositoryInserter,
        final Map<CommitTreePath, Map<CommitTreePath, CommitTreeEntry>> treeHierarchy,
        final ChangesetCommitItemReader previousChangesetCommitReader,
        final Item item,
        final TaskProgressMonitor progressMonitor)
        throws Exception
    {
        Check.notNull(repositoryInserter, "repositoryInserter"); //$NON-NLS-1$
        Check.notNull(treeHierarchy, "treeHierarchy"); //$NON-NLS-1$
        Check.notNull(previousChangesetCommitReader, "previousChangesetCommitReader"); //$NON-NLS-1$
        Check.notNull(item, "item"); //$NON-NLS-1$
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        if (item.getItemType() == ItemType.FOLDER)
        {
            return;
        }

        File tempFile = null;
        InputStream tempInputStream = null;
        ObjectId blobID = null;

        try
        {
            blobID = previousChangesetCommitReader.getFileObjectId(item.getServerItem(), item.getChangeSetID());

            if (blobID == null || ObjectId.equals(blobID, ObjectId.zeroId()))
            {
                tempFile = File.createTempFile(GitTFConstants.GIT_TF_NAME, null, tempDir);

                try
                {
                    versionControlService.downloadFile(item, tempFile.getAbsolutePath());
                }
                catch (VersionControlException e)
                {
                    // if the user is denied read permissions on the file an
                    // exception will be thrown here.

                    final String itemName = item.getServerItem() == null ? "" : item.getServerItem(); //$NON-NLS-1$

                    progressMonitor.displayWarning(Messages.formatString(
                        "CreateCommitForChangesetVersionSpecTask.NoContentDueToPermissionOrDestroyFormat", //$NON-NLS-1$
                        itemName));

                    log.error(e);

                    return;
                }
                catch (Throwable e)
                {
                    /*
                     * A workaround for unexpected TFS server errors. Aged
                     * servers with long history might have some items corrupted
                     * in some change sets and return incorrect HTTP response to
                     * the download request.
                     * 
                     * In this case we'd better suppress the error and assume
                     * that the file does not exist. The next change set that
                     * contains this file will commit it into the repository. It
                     * seems to better to miss something in the file's history
                     * log rather than to fail cloning the repository entirely.
                     */

                    // TODO: We should make this behavior optional since it's a
                    // bit dangerous. If the file download fails in its last
                    // change set the file will be marked as deleted in the
                    // repository.

                    if (e instanceof IOException)
                    {
                        throw (Exception) e;
                    }
                    else
                    {
                        final String itemName = item.getServerItem() == null ? "???" : item.getServerItem(); //$NON-NLS-1$
                        final String changeSetID =
                            item.getChangeSetID() == 0 ? "???" : Integer.toString(item.getChangeSetID()); //$NON-NLS-1$
                        final String checkinDate =
                            item.getCheckinDate() == null ? "???" : item.getCheckinDate().toString(); //$NON-NLS-1$

                        final String message =
                            Messages.formatString("CreateCommitForChangesetVersionSpecTask.UnexpectedErrorFormat", //$NON-NLS-1$
                                itemName,
                                changeSetID,
                                checkinDate);

                        progressMonitor.displayWarning(message);
                        progressMonitor.displayWarning(e.getMessage());
                        progressMonitor.displayWarning(Messages.getString("CreateCommitForChangesetVersionSpecTask.SeeLog")); //$NON-NLS-1$

                        log.warn(message);
                        log.error(e);
                    }
                }

                if (tempFile.exists())
                {
                    tempInputStream = new FileInputStream(tempFile);
                    blobID = repositoryInserter.insert(OBJ_BLOB, tempFile.length(), tempInputStream);
                }
                else
                {
                    blobID = ObjectId.zeroId();
                }
            }

            FileMode fileMode = FileMode.REGULAR_FILE;

            /* handle executable files */
            if (item.getPropertyValues() != null)
            {
                if (PropertyConstants.EXECUTABLE_ENABLED_VALUE.equals(PropertyUtils.selectMatching(
                    item.getPropertyValues(),
                    PropertyConstants.EXECUTABLE_KEY)))
                {
                    fileMode = FileMode.EXECUTABLE_FILE;
                }
            }

            createBlob(repositoryInserter, treeHierarchy, item.getServerItem(), blobID, fileMode, progressMonitor);
        }
        finally
        {
            if (tempInputStream != null)
            {
                tempInputStream.close();
            }

            if (tempFile != null)
            {
                tempFile.delete();
            }
        }
    }

    private String getMentions()
    {
        if (witClient == null)
        {
            return ""; //$NON-NLS-1$
        }

        final WorkItem[] workItems = getChangesetWorkItems(changesetID);
        if (workItems == null)
        {
            return ""; //$NON-NLS-1$
        }

        final StringBuilder sb = new StringBuilder();

        for (final WorkItem workItem : workItems)
        {
            addWorkItem(sb, workItem);
        }

        return sb.toString();
    }

    public Item[] getCommittedItems()
    {
        return committedItems;
    }

    private void addWorkItem(final StringBuilder sb, final WorkItem workItem)
    {
        sb.append(NEWLINE);
        sb.append(HASH);

        final String itemID = Integer.toString(workItem.getID());
        sb.append(itemID);
        sb.append(SPACES.substring(0, Math.max(1, WIT_TITLE_PAD_WIDTH - itemID.length())));

        sb.append(workItem.getFields().getField(CoreFieldReferenceNames.TITLE).getValue());
    }

    private ObjectId createCommit(
        final ObjectInserter repositoryInserter,
        final ObjectId rootTree,
        final Changeset changeset)
        throws IOException
    {
        Check.notNull(changeset, "changeset"); //$NON-NLS-1$
        Check.notNull(repositoryInserter, "repositoryInserter"); //$NON-NLS-1$
        Check.notNull(rootTree, "rootTree"); //$NON-NLS-1$

        return createCommit(
            repositoryInserter,
            rootTree,
            parentCommitID,
            changeset.getOwnerDisplayName(),
            changeset.getOwner(),
            changeset.getCommitterDisplayName(),
            changeset.getCommitter(),
            changeset.getDate(),
            changeset.getComment() + getMentions());
    }

    private class ChangesetCommitItemReader
    {
        private boolean initialized = false;

        private final int changesetID;
        private final ObjectId commitId;

        private RevTree commitRevTree;
        private ObjectReader objectReader;
        private final Item[] committedItems;

        private Map<String, Integer> changesetItems;

        public ChangesetCommitItemReader(final int changesetId, final ObjectId commitId, final Item[] committedItems)
        {
            this.changesetID = changesetId;
            this.commitId = commitId;
            this.committedItems = committedItems;
        }

        public ObjectId getFileObjectId(final String itemServerPath, final int requestedVersion)
        {
            if (!initialized)
            {
                initialize();
            }

            if (commitRevTree == null || objectReader == null)
            {
                return null;
            }

            TreeWalk file;
            try
            {
                if (commitContainsFileAtVersion(itemServerPath, requestedVersion))
                {
                    file =
                        TreeWalk.forPath(
                            objectReader,
                            ServerPath.makeRelative(itemServerPath, serverPath),
                            commitRevTree);

                    if (file == null)
                    {
                        return null;
                    }

                    return file.getObjectId(0);
                }
            }
            catch (Exception e)
            {
                // if we cannot read the object then we do not need to optimize
                // the call
            }

            return null;
        }

        private void initialize()
        {
            Check.isTrue(initialized == false, "initialized == false"); //$NON-NLS-1$

            initialized = true;

            if (commitId != null)
            {
                final RevWalk walker = new RevWalk(repository);

                try
                {
                    final RevCommit revCommit = walker.parseCommit(commitId);
                    commitRevTree = revCommit.getTree();
                    objectReader = repository.newObjectReader();
                }
                catch (Exception e)
                {
                    // if we cannot read the object then we do not need to
                    // optimize the call
                    commitRevTree = null;
                    objectReader = null;

                    return;
                }
                finally
                {
                    if (walker != null)
                    {
                        walker.release();
                    }
                }

                changesetItems = new HashMap<String, Integer>(committedItems.length);
                for (final Item item : committedItems)
                {
                    changesetItems.put(item.getServerItem().toLowerCase(), item.getChangeSetID());
                }
            }
        }

        private boolean commitContainsFileAtVersion(final String filePath, final int requestedVersion)
        {
            final String filePathKey = filePath.toLowerCase();

            if (changesetItems.containsKey(filePathKey))
            {
                final int changesetVersionInChangeset = changesetItems.get(filePathKey);
                return changesetVersionInChangeset == requestedVersion;
            }

            return false;
        }
    }

    public WorkItem[] getChangesetWorkItems(final int changesetID)
    {
        if (witClient == null)
        {
            return null;
        }

        final ArtifactID changesetArtifactId = ArtifactIDFactory.newChangesetArtifactID(changesetID);

        final Query query = witClient.createReferencingQuery(changesetArtifactId.encodeURI());
        query.getDisplayFieldList().add(CoreFieldReferenceNames.TITLE);

        final WorkItemCollection collection = query.runQuery();
        final WorkItem[] workItems = new WorkItem[collection.size()];

        for (int i = 0; i < collection.size(); i++)
        {
            workItems[i] = collection.getWorkItem(i);
        }

        return workItems;
    }

}
