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

import java.net.URI;
import java.util.Calendar;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.junit.Test;

import com.microsoft.gittf.core.mock.MockChangesetProperties;
import com.microsoft.gittf.core.mock.MockVersionControlService;
import com.microsoft.gittf.core.tasks.framework.NullTaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.test.Util;
import com.microsoft.gittf.core.util.RepositoryUtil;

public class FetchTaskTest
    extends TestCase
{
    protected void setUp()
        throws Exception
    {
        Util.setUp(getName());
    }

    protected void tearDown()
        throws Exception
    {
        Util.tearDown(getName());
    }

    @Test
    public void testFetchShallow()
        throws Exception
    {
        URI projectCollectionURI = new URI("http://fakeCollection:8080/tfs/DefaultCollection"); //$NON-NLS-1$
        String tfsPath = "$/project"; //$NON-NLS-1$
        String gitRepositoryPath = Util.getRepositoryFile(getName()).getAbsolutePath();

        final MockVersionControlService mockVersionControlService = new MockVersionControlService();

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

        final Repository repository = RepositoryUtil.createNewRepository(gitRepositoryPath, false);

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

        FetchTask fetchTask = new FetchTask(repository, mockVersionControlService);
        TaskStatus fetchTaskStatus = fetchTask.run(new NullTaskProgressMonitor());

        // Verify task completed without errors
        assertTrue(fetchTaskStatus.isOK());

        // Verify the commit fetched
        Ref fetchHeadRef = repository.getRef(Constants.FETCH_HEAD);
        Ref headRef = repository.getRef(Constants.HEAD);

        assertNotNull(fetchHeadRef);
        assertNotNull(headRef);

        ObjectId fetchHeadCommitID = fetchHeadRef.getObjectId();
        ObjectId headCommitID = headRef.getObjectId();

        RevWalk revWalk = new RevWalk(repository);
        RevCommit fetchedCommit = revWalk.parseCommit(fetchHeadCommitID);
        RevCommit headCommit = revWalk.parseCommit(headCommitID);

        assertEquals(fetchedCommit.getFullMessage(), "comment4"); //$NON-NLS-1$

        PersonIdent ownwer = fetchedCommit.getAuthorIdent();
        assertEquals(ownwer.getEmailAddress(), "ownerName4"); //$NON-NLS-1$
        assertEquals(ownwer.getName(), "ownerDisplayName4"); //$NON-NLS-1$

        PersonIdent committer = fetchedCommit.getCommitterIdent();
        assertEquals(committer.getEmailAddress(), "committerName4"); //$NON-NLS-1$
        assertEquals(committer.getName(), "committerDisplayName4"); //$NON-NLS-1$

        // Verify the file content
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.setRecursive(true);

        treeWalk.addTree(headCommit.getTree());
        treeWalk.addTree(fetchedCommit.getTree());

        treeWalk.setFilter(TreeFilter.ANY_DIFF);

        int count = 0;
        while (treeWalk.next())
        {
            ObjectId fileObjectId = treeWalk.getObjectId(1);
            byte[] fileContent = repository.getObjectDatabase().open(fileObjectId, OBJ_BLOB).getBytes();

            switch (count)
            {
                case 0:
                    assertTrue(mockVersionControlService.verifyFileContent(fileContent, "$/project/folder/file1.txt", //$NON-NLS-1$
                        4));
                    break;
                case 2:
                    assertTrue(mockVersionControlService.verifyFileContent(fileContent, "$/project/folder2/file1.txt", //$NON-NLS-1$
                        4));
                    break;
                case 1:
                    assertTrue(mockVersionControlService.verifyFileContent(
                        fileContent,
                        "$/project/folder/nestedFolder/file1.txt", //$NON-NLS-1$
                        4));
                    break;
            }

            count++;
        }

        Git git = new Git(repository);

        // Verify the tags
        List<Ref> tags = git.tagList().call();
        assertEquals(2, tags.size());
    }

    @Test
    public void testFetchDeep()
        throws Exception
    {
        URI projectCollectionURI = new URI("http://fakeCollection:8080/tfs/DefaultCollection"); //$NON-NLS-1$
        String tfsPath = "$/project"; //$NON-NLS-1$
        String gitRepositoryPath = Util.getRepositoryFile(getName()).getAbsolutePath();

        final MockVersionControlService mockVersionControlService = new MockVersionControlService();

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

        final Repository repository = RepositoryUtil.createNewRepository(gitRepositoryPath, false);

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

        mockVersionControlService.AddFile("$/project/folder/file1.txt", 5); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/folder2/file1.txt", 5); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/folder/nestedFolder/file1.txt", 5); //$NON-NLS-1$

        MockChangesetProperties changesetProperties3 = new MockChangesetProperties("ownerDisplayName5", //$NON-NLS-1$
            "ownerName5", //$NON-NLS-1$
            "committerDisplayName5", //$NON-NLS-1$
            "committerName5", //$NON-NLS-1$
            "comment5", //$NON-NLS-1$
            date);
        mockVersionControlService.updateChangesetInformation(changesetProperties3, 5);

        FetchTask fetchTask = new FetchTask(repository, mockVersionControlService);
        fetchTask.setDeep(true);
        TaskStatus fetchTaskStatus = fetchTask.run(new NullTaskProgressMonitor());

        // Verify task completed without errors
        assertTrue(fetchTaskStatus.isOK());

        // Retrieve commits
        Ref fetchHeadRef = repository.getRef(Constants.FETCH_HEAD);
        Ref headRef = repository.getRef(Constants.HEAD);

        assertNotNull(fetchHeadRef);
        assertNotNull(headRef);

        ObjectId fetchHeadCommitID = fetchHeadRef.getObjectId();
        ObjectId headCommitID = headRef.getObjectId();

        RevWalk revWalk = new RevWalk(repository);
        RevCommit fetchedCommit = revWalk.parseCommit(fetchHeadCommitID);
        RevCommit headCommit = revWalk.parseCommit(headCommitID);

        assertEquals(fetchedCommit.getParentCount(), 1);

        RevCommit intermediateCommit = revWalk.parseCommit(fetchedCommit.getParent(0).getId());

        // Verify intermediateCommit
        assertEquals(intermediateCommit.getFullMessage(), "comment4"); //$NON-NLS-1$

        PersonIdent ownwer = intermediateCommit.getAuthorIdent();
        assertEquals(ownwer.getEmailAddress(), "ownerName4"); //$NON-NLS-1$
        assertEquals(ownwer.getName(), "ownerDisplayName4"); //$NON-NLS-1$

        PersonIdent committer = intermediateCommit.getCommitterIdent();
        assertEquals(committer.getEmailAddress(), "committerName4"); //$NON-NLS-1$
        assertEquals(committer.getName(), "committerDisplayName4"); //$NON-NLS-1$

        // Verify fetch_head commit
        assertEquals(fetchedCommit.getFullMessage(), "comment5"); //$NON-NLS-1$

        ownwer = fetchedCommit.getAuthorIdent();
        assertEquals(ownwer.getEmailAddress(), "ownerName5"); //$NON-NLS-1$
        assertEquals(ownwer.getName(), "ownerDisplayName5"); //$NON-NLS-1$

        committer = fetchedCommit.getCommitterIdent();
        assertEquals(committer.getEmailAddress(), "committerName5"); //$NON-NLS-1$
        assertEquals(committer.getName(), "committerDisplayName5"); //$NON-NLS-1$

        // Verify the file content
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.setRecursive(true);

        treeWalk.addTree(headCommit.getTree());
        treeWalk.addTree(intermediateCommit.getTree());

        treeWalk.setFilter(TreeFilter.ANY_DIFF);

        int count = 0;
        while (treeWalk.next())
        {
            ObjectId fileObjectId = treeWalk.getObjectId(1);
            byte[] fileContent = repository.getObjectDatabase().open(fileObjectId, OBJ_BLOB).getBytes();

            switch (count)
            {
                case 0:
                    assertTrue(mockVersionControlService.verifyFileContent(fileContent, "$/project/folder/file1.txt", //$NON-NLS-1$
                        4));
                    break;
                case 2:
                    assertTrue(mockVersionControlService.verifyFileContent(fileContent, "$/project/folder2/file1.txt", //$NON-NLS-1$
                        4));
                    break;
                case 1:
                    assertTrue(mockVersionControlService.verifyFileContent(
                        fileContent,
                        "$/project/folder/nestedFolder/file1.txt", //$NON-NLS-1$
                        4));
                    break;
            }

            count++;
        }

        treeWalk.reset();
        treeWalk.setRecursive(true);

        treeWalk.addTree(headCommit.getTree());
        treeWalk.addTree(fetchedCommit.getTree());

        treeWalk.setFilter(TreeFilter.ANY_DIFF);

        count = 0;
        while (treeWalk.next())
        {
            ObjectId fileObjectId = treeWalk.getObjectId(1);
            byte[] fileContent = repository.getObjectDatabase().open(fileObjectId, OBJ_BLOB).getBytes();

            switch (count)
            {
                case 0:
                    assertTrue(mockVersionControlService.verifyFileContent(fileContent, "$/project/folder/file1.txt", //$NON-NLS-1$
                        5));
                    break;
                case 2:
                    assertTrue(mockVersionControlService.verifyFileContent(fileContent, "$/project/folder2/file1.txt", //$NON-NLS-1$
                        5));
                    break;
                case 1:
                    assertTrue(mockVersionControlService.verifyFileContent(
                        fileContent,
                        "$/project/folder/nestedFolder/file1.txt", //$NON-NLS-1$
                        5));
                    break;
            }

            count++;
        }

        Git git = new Git(repository);

        // Verify the tags
        List<Ref> tags = git.tagList().call();
        assertEquals(3, tags.size());
    }
}
