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
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;

import junit.framework.TestCase;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;

import com.microsoft.gittf.core.mock.MockChangesetProperties;
import com.microsoft.gittf.core.mock.MockVersionControlService;
import com.microsoft.gittf.core.tasks.framework.NullTaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.test.Util;
import com.microsoft.gittf.core.util.RepositoryUtil;

public class PullTaskTest
    extends TestCase
{
    private MockVersionControlService mockVersionControlService;
    private Repository repository;

    private ObjectId gitChangeCommitId;

    protected void setUp()
        throws Exception
    {
        Util.setUp(getName());
        prepareRepo();
    }

    protected void tearDown()
        throws Exception
    {
        Util.tearDown(getName());
    }

    @Test
    public void testPullMergeResolve()
        throws Exception
    {
        runPullTask(MergeStrategy.RESOLVE);

        String gitRepositoryPath = repository.getWorkTree().getAbsolutePath();

        // Verify Changeset 4 content merged into head
        assertTrue(mockVersionControlService.verifyFileContent(new File(gitRepositoryPath, "project/folder/file1.txt"), //$NON-NLS-1$
            "$/project/folder/file1.txt", //$NON-NLS-1$
            4));

        assertTrue(mockVersionControlService.verifyFileContent(
            new File(gitRepositoryPath, "project/folder2/file1.txt"), //$NON-NLS-1$
            "$/project/folder2/file1.txt", //$NON-NLS-1$
            4));

        assertTrue(mockVersionControlService.verifyFileContent(new File(
            gitRepositoryPath,
            "project/folder/nestedFolder/file1.txt"), //$NON-NLS-1$
            "$/project/folder/nestedFolder/file1.txt", //$NON-NLS-1$
            4));

        // Verify git commit content is still in head
        assertTrue(Util.verifyFileContent(new File(gitRepositoryPath, "project/folder/file2.txt"), //$NON-NLS-1$
            Util.generateContentForFileInGit("project/folder/file2.txt"))); //$NON-NLS-1$

        assertTrue(Util.verifyFileContent(new File(gitRepositoryPath, "project/folder2/file2.txt"), //$NON-NLS-1$
            Util.generateContentForFileInGit("project/folder2/file2.txt"))); //$NON-NLS-1$

        assertTrue(Util.verifyFileContent(new File(gitRepositoryPath, "project/folder/nestedFolder/file2.txt"), //$NON-NLS-1$
            Util.generateContentForFileInGit("project/folder/nestedFolder/file2.txt"))); //$NON-NLS-1$
    }

    @Test
    public void testPullMergeOurs()
        throws Exception
    {
        runPullTask(MergeStrategy.OURS);

        String gitRepositoryPath = repository.getWorkTree().getAbsolutePath();

        // Verify Changeset 4 content not merged into head
        assertTrue(mockVersionControlService.verifyFileContent(new File(gitRepositoryPath, "project/folder/file1.txt"), //$NON-NLS-1$
            "$/project/folder/file1.txt", //$NON-NLS-1$
            2));

        assertTrue(mockVersionControlService.verifyFileContent(
            new File(gitRepositoryPath, "project/folder2/file1.txt"), //$NON-NLS-1$
            "$/project/folder2/file1.txt", //$NON-NLS-1$
            2));

        assertTrue(mockVersionControlService.verifyFileContent(new File(
            gitRepositoryPath,
            "project/folder/nestedFolder/file1.txt"), //$NON-NLS-1$
            "$/project/folder/nestedFolder/file1.txt", //$NON-NLS-1$
            2));

        // Verify git commit content is still in head
        assertTrue(Util.verifyFileContent(new File(gitRepositoryPath, "project/folder/file2.txt"), //$NON-NLS-1$
            Util.generateContentForFileInGit("project/folder/file2.txt"))); //$NON-NLS-1$

        assertTrue(Util.verifyFileContent(new File(gitRepositoryPath, "project/folder2/file2.txt"), //$NON-NLS-1$
            Util.generateContentForFileInGit("project/folder2/file2.txt"))); //$NON-NLS-1$

        assertTrue(Util.verifyFileContent(new File(gitRepositoryPath, "project/folder/nestedFolder/file2.txt"), //$NON-NLS-1$
            Util.generateContentForFileInGit("project/folder/nestedFolder/file2.txt"))); //$NON-NLS-1$
    }

    @Test
    public void testPullMergeThiers()
        throws Exception
    {
        runPullTask(MergeStrategy.THEIRS);

        String gitRepositoryPath = repository.getWorkTree().getAbsolutePath();

        // Verify Changeset 4 content merged into head
        assertTrue(mockVersionControlService.verifyFileContent(new File(gitRepositoryPath, "project/folder/file1.txt"), //$NON-NLS-1$
            "$/project/folder/file1.txt", //$NON-NLS-1$
            4));

        assertTrue(mockVersionControlService.verifyFileContent(
            new File(gitRepositoryPath, "project/folder2/file1.txt"), //$NON-NLS-1$
            "$/project/folder2/file1.txt", //$NON-NLS-1$
            4));

        assertTrue(mockVersionControlService.verifyFileContent(new File(
            gitRepositoryPath,
            "project/folder/nestedFolder/file1.txt"), //$NON-NLS-1$
            "$/project/folder/nestedFolder/file1.txt", //$NON-NLS-1$
            4));

        // Verify Changeset 3 content and not the git changes
        assertTrue(mockVersionControlService.verifyFileContent(new File(gitRepositoryPath, "project/folder/file2.txt"), //$NON-NLS-1$
            "$/project/folder/file2.txt", //$NON-NLS-1$
            3));

        assertTrue(mockVersionControlService.verifyFileContent(
            new File(gitRepositoryPath, "project/folder2/file2.txt"), //$NON-NLS-1$
            "$/project/folder2/file2.txt", //$NON-NLS-1$
            3));

        assertTrue(mockVersionControlService.verifyFileContent(new File(
            gitRepositoryPath,
            "project/folder/nestedFolder/file2.txt"), //$NON-NLS-1$
            "$/project/folder/nestedFolder/file2.txt", //$NON-NLS-1$
            3));
    }

    @Test
    public void testPullRebase()
        throws Exception
    {
        runPullRebaseTask();

        String gitRepositoryPath = repository.getWorkTree().getAbsolutePath();

        // Verify Changeset 4 content merged into head
        assertTrue(mockVersionControlService.verifyFileContent(new File(gitRepositoryPath, "project/folder/file1.txt"), //$NON-NLS-1$
            "$/project/folder/file1.txt", //$NON-NLS-1$
            4));

        assertTrue(mockVersionControlService.verifyFileContent(
            new File(gitRepositoryPath, "project/folder2/file1.txt"), //$NON-NLS-1$
            "$/project/folder2/file1.txt", //$NON-NLS-1$
            4));

        assertTrue(mockVersionControlService.verifyFileContent(new File(
            gitRepositoryPath,
            "project/folder/nestedFolder/file1.txt"), //$NON-NLS-1$
            "$/project/folder/nestedFolder/file1.txt", //$NON-NLS-1$
            4));

        // Verify git commit content is still in head
        assertTrue(Util.verifyFileContent(new File(gitRepositoryPath, "project/folder/file2.txt"), //$NON-NLS-1$
            Util.generateContentForFileInGit("project/folder/file2.txt"))); //$NON-NLS-1$

        assertTrue(Util.verifyFileContent(new File(gitRepositoryPath, "project/folder2/file2.txt"), //$NON-NLS-1$
            Util.generateContentForFileInGit("project/folder2/file2.txt"))); //$NON-NLS-1$

        assertTrue(Util.verifyFileContent(new File(gitRepositoryPath, "project/folder/nestedFolder/file2.txt"), //$NON-NLS-1$
            Util.generateContentForFileInGit("project/folder/nestedFolder/file2.txt"))); //$NON-NLS-1$
    }

    private void runPullRebaseTask()
        throws IOException
    {
        // Run the pull task
        PullTask pullTask = new PullTask(repository, mockVersionControlService);
        pullTask.setRebase(true);

        TaskStatus pullTaskStatus = pullTask.run(new NullTaskProgressMonitor());

        // Verify task completed without errors
        assertTrue(pullTaskStatus.isOK());

        // Verify that the repo is in a good state
        assertTrue(repository.getRepositoryState() == RepositoryState.SAFE);

        // Verify rebase happened
        ObjectId fetchedCommitId = pullTask.getCommitId();

        Ref head = repository.getRef(Constants.HEAD);
        RevWalk revWalk = new RevWalk(repository);
        RevCommit headCommit = revWalk.parseCommit(head.getObjectId());

        assertEquals(headCommit.getParentCount(), 1);

        RevCommit parent = headCommit.getParents()[0];

        assertTrue(parent.getId().equals(fetchedCommitId));
    }

    private void runPullTask(MergeStrategy strategy)
        throws IOException
    {
        // Run the pull task
        PullTask pullTask = new PullTask(repository, mockVersionControlService);
        pullTask.setStrategy(strategy);

        TaskStatus pullTaskStatus = pullTask.run(new NullTaskProgressMonitor());

        // Verify task completed without errors
        assertTrue(pullTaskStatus.isOK());

        ObjectId fetchedCommitId = pullTask.getCommitId();

        // Verify the merge is completed successfully
        Ref head = repository.getRef(Constants.HEAD);

        RevWalk revWalk = new RevWalk(repository);
        RevCommit headCommit = revWalk.parseCommit(head.getObjectId());

        assertEquals(headCommit.getParentCount(), 2);

        RevCommit[] parents = headCommit.getParents();

        assertTrue((parents[0].getId().equals(fetchedCommitId) && parents[1].getId().equals(gitChangeCommitId))
            || (parents[0].getId().equals(gitChangeCommitId) && parents[1].getId().equals(fetchedCommitId)));
    }

    private void prepareRepo()
        throws Exception
    {
        URI projectCollectionURI = new URI("http://fakeCollection:8080/tfs/DefaultCollection"); //$NON-NLS-1$
        String tfsPath = "$/"; //$NON-NLS-1$
        String gitRepositoryPath = Util.getRepositoryFile(getName()).getAbsolutePath();

        mockVersionControlService = new MockVersionControlService();

        mockVersionControlService.AddFile("$/project/folder/file0.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/folder2/file0.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/folder/nestedFolder/file0.txt", 1); //$NON-NLS-1$

        mockVersionControlService.AddFile("$/project/folder/file1.txt", 2); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/folder2/file1.txt", 2); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/folder/nestedFolder/file1.txt", 2); //$NON-NLS-1$

        mockVersionControlService.AddFile("$/project/folder/file2.txt", 3); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/folder2/file2.txt", 3); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/folder/nestedFolder/file2.txt", 3); //$NON-NLS-1$

        Calendar date = Calendar.getInstance();
        date.set(2012, 11, 12, 18, 15);

        MockChangesetProperties changesetProperties = new MockChangesetProperties("ownerDisplayName", //$NON-NLS-1$
            "ownerName", //$NON-NLS-1$
            "committerDisplayName", //$NON-NLS-1$
            "committerName", //$NON-NLS-1$
            "comment", //$NON-NLS-1$
            date);
        mockVersionControlService.updateChangesetInformation(changesetProperties, 3);

        repository = RepositoryUtil.createNewRepository(gitRepositoryPath, false);

        CloneTask cloneTask = new CloneTask(projectCollectionURI, mockVersionControlService, tfsPath, repository);
        TaskStatus cloneTaskStatus = cloneTask.run(new NullTaskProgressMonitor());

        // Verify task completed without errors
        assertTrue(cloneTaskStatus.isOK());

        // Update some files
        mockVersionControlService.AddFile("$/project/folder/file1.txt", 4); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/folder2/file1.txt", 4); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/folder/nestedFolder/file1.txt", 4); //$NON-NLS-1$

        MockChangesetProperties changesetProperties2 = new MockChangesetProperties("ownerDisplayName4", //$NON-NLS-1$
            "ownerName4", //$NON-NLS-1$
            "committerDisplayName4", //$NON-NLS-1$
            "committerName4", //$NON-NLS-1$
            "comment4", //$NON-NLS-1$
            date);
        mockVersionControlService.updateChangesetInformation(changesetProperties2, 4);

        writeFileContentInGit("project/folder/file2.txt"); //$NON-NLS-1$
        writeFileContentInGit("project/folder2/file2.txt"); //$NON-NLS-1$
        writeFileContentInGit("project/folder/nestedFolder/file2.txt"); //$NON-NLS-1$

        RevCommit revCommit = new Git(repository).commit().setAll(true).setMessage("git-tf pull test").call(); //$NON-NLS-1$
        assertNotNull(revCommit);

        gitChangeCommitId = revCommit.toObjectId();
    }

    private void writeFileContentInGit(String filePath)
        throws IOException
    {
        File file = new File(repository.getWorkTree(), filePath);

        FileWriter fw = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fw);

        out.write(Util.generateContentForFileInGit(filePath));

        out.close();
    }
}
