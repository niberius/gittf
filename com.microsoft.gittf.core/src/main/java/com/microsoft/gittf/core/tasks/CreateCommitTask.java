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
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.interfaces.VersionControlService;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.DirectoryUtil;
import com.microsoft.gittf.core.util.RepositoryPath;
import com.microsoft.gittf.core.util.tree.CommitTreeEntry;
import com.microsoft.gittf.core.util.tree.CommitTreePath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import org.eclipse.jgit.lib.*;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;
import static org.eclipse.jgit.lib.Constants.OBJ_TREE;

public abstract class CreateCommitTask
        extends Task {
    protected final Repository repository;
    protected final VersionControlService versionControlService;
    protected final ObjectId parentCommitID;

    protected final String serverPath;
    protected final File tempDir;

    protected ObjectId commitId;

    public CreateCommitTask(
            final Repository repository,
            final VersionControlService versionControlClient,
            ObjectId parentCommitID) {
        Check.notNull(repository, "repository");
        Check.notNull(versionControlClient, "versionControlClient");

        this.repository = repository;
        this.versionControlService = versionControlClient;
        this.parentCommitID = parentCommitID;

        final GitTFConfiguration configuration = GitTFConfiguration.loadFrom(repository);
        Check.notNull(configuration, "configuration");

        this.serverPath = configuration.getServerPath();
        Check.notNullOrEmpty(serverPath, "serverPath");

        /* Set up a temporary directory */
        this.tempDir = DirectoryUtil.getTempDir(repository);
        Check.notNull(tempDir, "tempDir");

    }

    public ObjectId getCommitID() {
        return commitId;
    }

    protected void validateTempDirectory()
            throws Exception {
        /* Sanity-check the temporary directory */
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                throw new Exception(Messages.formatString("CreateCommitTask.ErrorCreatingTempDirectoryMessageFormat",
                        tempDir.getAbsolutePath()));
            }
        }

        if (!tempDir.isDirectory()) {
            throw new Exception(Messages.formatString("CreateCommitTask.ErrorCreatingTempDirectoryMessageFormat",
                    tempDir.getAbsolutePath()));
        }
    }

    protected void createBlob(
            final ObjectInserter repositoryInserter,
            final Map<CommitTreePath, Map<CommitTreePath, CommitTreeEntry>> treeHierarchy,
            final String serverItemPath,
            final ObjectId blobID,
            final FileMode fileMode,
            final TaskProgressMonitor progressMonitor)
            throws Exception {
        Check.notNull(repositoryInserter, "repositoryInserter");
        Check.notNull(treeHierarchy, "treeHierarchy");
        Check.notNull(serverItemPath, "serverItemPath");
        Check.notNull(blobID, "blobID");
        Check.notNull(progressMonitor, "progressMonitor");

        String folderName = ServerPath.makeRelative(ServerPath.getParent(serverItemPath), serverPath);
        String fileName = ServerPath.getFileName(serverItemPath);

        progressMonitor.setDetail(fileName);

        addToTreeHierarchy(treeHierarchy, folderName);

        Map<CommitTreePath, CommitTreeEntry> parentTree = treeHierarchy.get(new CommitTreePath(folderName, OBJ_TREE));

        if (parentTree == null) {
            throw new RuntimeException(Messages.formatString("CreateCommitTask.CouldNotLocateParentTreeFormat",
                    folderName));
        }

        parentTree.put(new CommitTreePath(fileName, OBJ_BLOB), new CommitTreeEntry(fileMode, blobID));
    }

    protected void addToTreeHierarchy(
            final Map<CommitTreePath, Map<CommitTreePath, CommitTreeEntry>> treeHierarchy,
            final String folderPath)
            throws Exception {
        final CommitTreePath folderKey = new CommitTreePath(folderPath, OBJ_TREE);

        if (treeHierarchy.containsKey(folderKey)) {
            return;
        }

        treeHierarchy.put(folderKey, new TreeMap<CommitTreePath, CommitTreeEntry>());

        if (folderPath.length() == 0) {
            return;
        }

        int separatorIdx = folderPath.lastIndexOf(RepositoryPath.PREFERRED_SEPARATOR_CHARACTER);
        String folderParent = (separatorIdx > 0) ? folderPath.substring(0, separatorIdx) : "";

        addToTreeHierarchy(treeHierarchy, folderParent);
    }

    protected ObjectId createTrees(
            final ObjectInserter repositoryInserter,
            final Map<CommitTreePath, Map<CommitTreePath, CommitTreeEntry>> treeHierarchy)
            throws IOException {
        Check.notNull(repositoryInserter, "repositoryInserter");
        Check.notNull(treeHierarchy, "treeHierarchy");

        /*
         * if their at at least one file in the directory then we link the
         * hierarchies together - otherwise this is an empty folder thus we need
         * to create an empty tree
         */
        if (treeHierarchy.size() > 0) {
            /* Link up trees with their parents */
            for (Entry<CommitTreePath, Map<CommitTreePath, CommitTreeEntry>> treeEntry : treeHierarchy.entrySet()) {
                String repoRelativeName = treeEntry.getKey().getName();
                Map<CommitTreePath, CommitTreeEntry> thisTree = treeEntry.getValue();

                /* Ignore the repository root directory */
                if (repoRelativeName.length() == 0) {
                    continue;
                }

                String folderName =
                        repoRelativeName.indexOf(RepositoryPath.PREFERRED_SEPARATOR_CHARACTER) >= 0
                                ? RepositoryPath.getParent(repoRelativeName) : "";

                String fileName = RepositoryPath.getFileName(repoRelativeName);

                Map<CommitTreePath, CommitTreeEntry> parentTree =
                        treeHierarchy.get(new CommitTreePath(folderName, OBJ_TREE));

                if (parentTree == null) {
                    throw new RuntimeException(Messages.formatString("CreateCommitTask.CouldNotLocateParentTreeFormat",
                            folderName));
                }

                ObjectId thisTreeID = createTree(repositoryInserter, thisTree);

                if (!ObjectId.zeroId().equals(thisTreeID)) {
                    parentTree.put(new CommitTreePath(fileName, OBJ_TREE), new CommitTreeEntry(
                            FileMode.TREE,
                            thisTreeID));
                }
            }

            /* Add the root tree */
            Map<CommitTreePath, CommitTreeEntry> rootTree = treeHierarchy.get(new CommitTreePath("", OBJ_TREE));

            if (rootTree == null) {
                throw new RuntimeException(Messages.getString("CreateCommitTask.CouldNotLocateRootTree"));
            }

            return createTree(repositoryInserter, rootTree);
        } else {
            return createEmptyTree(repositoryInserter);
        }
    }

    private ObjectId createTree(final ObjectInserter repositoryInserter, final Map<CommitTreePath, CommitTreeEntry> tree)
            throws IOException {
        if (tree.isEmpty()) {
            return createEmptyTree(repositoryInserter);
        }

        TreeFormatter treeFormatter = new TreeFormatter();

        for (Entry<CommitTreePath, CommitTreeEntry> entry : tree.entrySet()) {
            String name = entry.getKey().getName();
            FileMode mode = entry.getValue().getFileMode();
            ObjectId objectId = entry.getValue().getObjectID();

            treeFormatter.append(name, mode, objectId);
        }

        return treeFormatter.insertTo(repositoryInserter);
    }

    protected ObjectId createEmptyTree(ObjectInserter repositoryInserter)
            throws IOException {
        TreeFormatter treeFormatter = new TreeFormatter();
        return treeFormatter.insertTo(repositoryInserter);
    }

    protected ObjectId createCommit(
            ObjectInserter repositoryInserter,
            ObjectId rootTree,
            ObjectId parentId,
            String authorName,
            String authorEmail,
            String committerName,
            String committerEmail,
            Calendar changesetDate,
            String commitMessage)
            throws IOException {
        Check.notNull(repositoryInserter, "repositoryInserter");
        Check.notNull(rootTree, "rootTree");

        CommitBuilder commit = new CommitBuilder();
        commit.setAuthor(new PersonIdent(authorName, authorEmail, changesetDate.getTime(), TimeZone.getDefault()));
        commit.setCommitter(new PersonIdent(
                committerName,
                committerEmail,
                changesetDate.getTime(),
                TimeZone.getDefault()));
        commit.setMessage(commitMessage);
        commit.setTreeId(rootTree);

        if (parentId != null) {
            commit.setParentId(parentId);
        }

        return repositoryInserter.insert(commit);
    }
}
