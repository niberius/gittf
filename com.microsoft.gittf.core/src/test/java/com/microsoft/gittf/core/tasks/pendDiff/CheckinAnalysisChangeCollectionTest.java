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

import java.io.File;
import java.net.URI;
import java.util.Calendar;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;

import com.microsoft.gittf.core.mock.MockChangesetProperties;
import com.microsoft.gittf.core.mock.MockVersionControlService;
import com.microsoft.gittf.core.tasks.CloneTask;
import com.microsoft.gittf.core.tasks.framework.NullTaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.test.Util;
import com.microsoft.gittf.core.util.RepositoryUtil;

public class CheckinAnalysisChangeCollectionTest
    extends TestCase
{
    private Repository repository = null;
    private RevCommit initialCommit = null;

    protected void setUp()
        throws Exception
    {
        Util.setUp(getName());
        initRepository();
    }

    protected void tearDown()
        throws Exception
    {
        Util.tearDown(getName());
        cleanupRepository();
    }

    /* Add Tests */

    /* Delete Tests */

    public void testFileDeletes()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/file2.txt").delete(); //$NON-NLS-1$

        RevCommit newCommit = commit();

        CheckinAnalysisChangeCollection analysis = buildCheckinAnalysis(newCommit);

        assertEquals(1, analysis.size());
        assertEquals(1, analysis.getDeletes().size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(analysis, "root/file2.txt")); //$NON-NLS-1$
    }

    public void testFileDeletes2()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/file2.txt").delete(); //$NON-NLS-1$
        new File(repository.getWorkTree(), "root/parent/file2.txt").delete(); //$NON-NLS-1$
        new File(repository.getWorkTree(), "root/parent/child/file2.txt").delete(); //$NON-NLS-1$
        new File(repository.getWorkTree(), "root/parent/child/grandChild/file2.txt").delete(); //$NON-NLS-1$
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file2.txt").delete(); //$NON-NLS-1$

        RevCommit newCommit = commit();

        CheckinAnalysisChangeCollection analysis = buildCheckinAnalysis(newCommit);

        assertEquals(5, analysis.size());
        assertEquals(5, analysis.getDeletes().size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(analysis, "root/file2.txt")); //$NON-NLS-1$

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(analysis, "root/parent/file2.txt")); //$NON-NLS-1$

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(analysis, "root/parent/child/file2.txt")); //$NON-NLS-1$

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(analysis, "root/parent/child/grandChild/file2.txt")); //$NON-NLS-1$

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            analysis,
            "root/parent/child/grandChild/greatGrandChild/file2.txt")); //$NON-NLS-1$
    }

    public void testFolderDeletes()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file1.txt").delete(); //$NON-NLS-1$
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file2.txt").delete(); //$NON-NLS-1$
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file3.txt").delete(); //$NON-NLS-1$

        RevCommit newCommit = commit();

        CheckinAnalysisChangeCollection analysis = buildCheckinAnalysis(newCommit);
        List<DeleteChange> deletes = analysis.getDeletes();

        assertEquals(4, analysis.size());
        assertEquals(4, deletes.size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            analysis,
            "root/parent/child/grandChild/greatGrandChild")); //$NON-NLS-1$
    }

    public void testFolderDeletes2()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file1.txt").delete(); //$NON-NLS-1$
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file2.txt").delete(); //$NON-NLS-1$
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file3.txt").delete(); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/child/grandChild/file1.txt").delete(); //$NON-NLS-1$
        new File(repository.getWorkTree(), "root/parent/child/grandChild/file2.txt").delete(); //$NON-NLS-1$
        new File(repository.getWorkTree(), "root/parent/child/grandChild/file3.txt").delete(); //$NON-NLS-1$

        RevCommit newCommit = commit();

        CheckinAnalysisChangeCollection analysis = buildCheckinAnalysis(newCommit);
        List<DeleteChange> deletes = analysis.getDeletes();

        assertEquals(7, analysis.size());
        assertEquals(7, deletes.size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(analysis, "root/parent/child/grandChild")); //$NON-NLS-1$
    }

    /* Edit Tests */

    /* Rename Tests */

    public void testFileRenames()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/file2.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/file2-rename.txt")); //$NON-NLS-1$

        add("root"); //$NON-NLS-1$

        RevCommit newCommit = commit();

        List<RenameChange> renames = buildRenames(newCommit);

        assertEquals(1, renames.size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(renames, "root/file2-rename.txt")); //$NON-NLS-1$
    }

    public void testFileRenames2()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/file2.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/file2-rename.txt")); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/file2.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/file2-parent-rename.txt")); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/child/file2.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/file2-parent-child-rename.txt")); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/child/grandChild/file2.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/file2-parent-child-grandChild-rename.txt")); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file2.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/file2-parent-child-grandChild-greatGrandChild-rename.txt")); //$NON-NLS-1$

        add("*"); //$NON-NLS-1$
        add("root"); //$NON-NLS-1$

        RevCommit newCommit = commit();

        List<RenameChange> renames = buildRenames(newCommit);

        assertEquals(5, renames.size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(renames, "root/file2-rename.txt")); //$NON-NLS-1$

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(renames, "root/file2-parent-rename.txt")); //$NON-NLS-1$

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(renames, "root/file2-parent-child-rename.txt")); //$NON-NLS-1$

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            renames,
            "root/file2-parent-child-grandChild-rename.txt")); //$NON-NLS-1$

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            renames,
            "root/file2-parent-child-grandChild-greatGrandChild-rename.txt")); //$NON-NLS-1$
    }

    public void testFileRenameLeavingEmptyFolder()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file1.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/file1-parent-child-grandChild-greatGrandChild-rename.txt")); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file2.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/file2-parent-child-grandChild-greatGrandChild-rename.txt")); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file3.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/file3-parent-child-grandChild-greatGrandChild-rename.txt")); //$NON-NLS-1$

        add("root"); //$NON-NLS-1$
        add("root/parent/child/grandChild"); //$NON-NLS-1$

        RevCommit newCommit = commit();

        List<RenameChange> renames = buildRenames(newCommit);

        assertEquals(3, renames.size());
    }

    /* Rename Edit Tests */

    public void testFileRenameEdit()
        throws Exception
    {
        File toTest = new File(repository.getWorkTree(), "root/file2.txt"); //$NON-NLS-1$

        Util.touchFile(toTest);
        toTest.renameTo(new File(repository.getWorkTree(), "root/file2-rename.txt")); //$NON-NLS-1$

        add("root"); //$NON-NLS-1$

        RevCommit newCommit = commit();

        List<RenameChange> renames = buildRenames(newCommit);

        assertEquals(1, renames.size());
        assertTrue(renames.get(0).isEdit());
        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(renames, "root/file2-rename.txt")); //$NON-NLS-1$
    }

    public void testFolderRename()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild-rename")); //$NON-NLS-1$

        add("root/parent/child/grandChild"); //$NON-NLS-1$

        RevCommit newCommit = commit();

        List<RenameChange> renames = buildRenames(newCommit);

        assertEquals(1, renames.size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            renames,
            "root/parent/child/grandChild/greatGrandChild-rename")); //$NON-NLS-1$
    }

    public void testFolderRenameEdit()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild-rename")); //$NON-NLS-1$

        File toTest =
            new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild-rename/file2.txt"); //$NON-NLS-1$
        Util.touchFile(toTest);

        add("root/parent/child/grandChild"); //$NON-NLS-1$

        RevCommit newCommit = commit();

        List<RenameChange> renames = buildRenames(newCommit);

        assertEquals(2, renames.size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            renames,
            "root/parent/child/grandChild/greatGrandChild-rename")); //$NON-NLS-1$

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            renames,
            "root/parent/child/grandChild/greatGrandChild-rename/file2.txt")); //$NON-NLS-1$

        assertTrue(((RenameChange) CheckinAnalysisChangeCollectionUtil.getChange(
            renames,
            "root/parent/child/grandChild/greatGrandChild-rename/file2.txt")).isEdit()); //$NON-NLS-1$
    }

    public void testFolderRenameWithParent()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/parent/child/grandChild") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/parent/child/grandChild-rename")); //$NON-NLS-1$

        add("root/parent/child"); //$NON-NLS-1$

        RevCommit newCommit = commit();

        List<RenameChange> renames = buildRenames(newCommit);

        assertEquals(1, renames.size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(renames, "root/parent/child/grandChild-rename")); //$NON-NLS-1$
    }

    public void testNestedFolderRename()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild-rename")); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/child/grandChild") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/parent/child/grandChild-rename")); //$NON-NLS-1$

        add("root/parent/child"); //$NON-NLS-1$

        RevCommit newCommit = commit();

        List<RenameChange> renames = buildRenames(newCommit);

        assertEquals(2, renames.size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(renames, "root/parent/child/grandChild-rename")); //$NON-NLS-1$
        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            renames,
            "root/parent/child/grandChild-rename/greatGrandChild-rename")); //$NON-NLS-1$
    }

    public void testNestedFolderRenameWithEdits()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild-rename")); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/child/grandChild") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/parent/child/grandChild-rename")); //$NON-NLS-1$

        File toTest =
            new File(repository.getWorkTree(), "root/parent/child/grandChild-rename/greatGrandChild-rename/file2.txt"); //$NON-NLS-1$
        Util.touchFile(toTest);

        add("root/parent/child"); //$NON-NLS-1$

        RevCommit newCommit = commit();

        List<RenameChange> renames = buildRenames(newCommit);

        assertEquals(3, renames.size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(renames, "root/parent/child/grandChild-rename")); //$NON-NLS-1$
        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            renames,
            "root/parent/child/grandChild-rename/greatGrandChild-rename")); //$NON-NLS-1$
        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            renames,
            "root/parent/child/grandChild-rename/greatGrandChild-rename/file2.txt")); //$NON-NLS-1$

        assertTrue(((RenameChange) CheckinAnalysisChangeCollectionUtil.getChange(
            renames,
            "root/parent/child/grandChild-rename/greatGrandChild-rename/file2.txt")).isEdit()); //$NON-NLS-1$
    }

    public void testFolderRenameToSubFolderWithEdits()
        throws Exception
    {
        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/subFolder").mkdirs(); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file1.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/subFolder/file1.txt")); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file2.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/subFolder/file2.txt")); //$NON-NLS-1$

        new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/file3.txt") //$NON-NLS-1$
        .renameTo(new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/subFolder/file3.txt")); //$NON-NLS-1$

        File toTest =
            new File(repository.getWorkTree(), "root/parent/child/grandChild/greatGrandChild/subFolder/file2.txt"); //$NON-NLS-1$
        Util.touchFile(toTest);

        add("root/parent/child/grandChild/greatGrandChild"); //$NON-NLS-1$

        RevCommit newCommit = commit();

        List<RenameChange> renames = buildRenames(newCommit);

        assertEquals(3, renames.size());

        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            renames,
            "root/parent/child/grandChild/greatGrandChild/subFolder/file1.txt")); //$NON-NLS-1$
        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            renames,
            "root/parent/child/grandChild/greatGrandChild/subFolder/file2.txt")); //$NON-NLS-1$
        assertTrue(CheckinAnalysisChangeCollectionUtil.contains(
            renames,
            "root/parent/child/grandChild/greatGrandChild/subFolder/file3.txt")); //$NON-NLS-1$

        assertTrue(((RenameChange) CheckinAnalysisChangeCollectionUtil.getChange(
            renames,
            "root/parent/child/grandChild/greatGrandChild/subFolder/file2.txt")).isEdit()); //$NON-NLS-1$
    }

    /* Utility */
    private void initRepository()
        throws Exception
    {
        URI projectCollectionURI = new URI("http://fakeCollection:8080/tfs/DefaultCollection"); //$NON-NLS-1$
        String tfsPath = "$/project"; //$NON-NLS-1$
        String gitRepositoryPath = Util.getRepositoryFile(getName()).getAbsolutePath();

        final MockVersionControlService mockVersionControlService = new MockVersionControlService();

        mockVersionControlService.AddFile("$/project/root/file1.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/file2.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/file3.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/file1.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/file2.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/file3.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/child/file1.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/child/file2.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/child/file3.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/child/grandChild/file1.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/child/grandChild/file2.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/child/grandChild/file3.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/child/grandChild/greatGrandChild/file1.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/child/grandChild/greatGrandChild/file2.txt", 1); //$NON-NLS-1$
        mockVersionControlService.AddFile("$/project/root/parent/child/grandChild/greatGrandChild/file3.txt", 1); //$NON-NLS-1$

        Calendar date = Calendar.getInstance();
        date.set(2012, 11, 12, 18, 15);
        MockChangesetProperties changesetProperties = new MockChangesetProperties("ownerDisplayName", //$NON-NLS-1$
            "ownerName", //$NON-NLS-1$
            "committerDisplayName", //$NON-NLS-1$
            "committerName", //$NON-NLS-1$
            "comment", //$NON-NLS-1$
            date);

        mockVersionControlService.updateChangesetInformation(changesetProperties, 1);

        repository = RepositoryUtil.createNewRepository(gitRepositoryPath, false);

        CloneTask cloneTask = new CloneTask(projectCollectionURI, mockVersionControlService, tfsPath, repository);
        TaskStatus cloneTaskStatus = cloneTask.run(new NullTaskProgressMonitor());

        // Verify task completed without errors
        assertTrue(cloneTaskStatus.isOK());

        Git git = new Git(repository);
        initialCommit = git.log().call().iterator().next();

        // Verify the first commit exists
        assertFalse(ObjectId.zeroId().equals(initialCommit));
    }

    private void cleanupRepository()
    {
        repository = null;
        initialCommit = null;
    }

    private void add(String folder)
        throws NoFilepatternException,
            GitAPIException
    {
        Git git = new Git(repository);

        git.add().setUpdate(false).addFilepattern(folder).call();
    }

    private RevCommit commit()
        throws NoHeadException,
            NoMessageException,
            UnmergedPathsException,
            ConcurrentRefUpdateException,
            WrongRepositoryStateException,
            GitAPIException
    {
        Git git = new Git(repository);

        return git.commit().setAll(true).setMessage("commit").call(); //$NON-NLS-1$
    }

    private List<RenameChange> buildRenames(RevCommit newCommit)
        throws Exception
    {
        RevTree fromTree = initialCommit.getTree();
        RevTree toTree = newCommit.getTree();

        CheckinAnalysisChangeCollection analysis =
            PendDifferenceTask.analyzeDifferences(
                repository,
                fromTree,
                toTree,
                RenameMode.ALL,
                new NullTaskProgressMonitor());

        TfsFolderRenameDetector folderRenameDetector = analysis.createFolderRenameDetector();
        folderRenameDetector.compute();

        return folderRenameDetector.getRenames();
    }

    private CheckinAnalysisChangeCollection buildCheckinAnalysis(RevCommit newCommit)
        throws Exception
    {
        RevTree fromTree = initialCommit.getTree();
        RevTree toTree = newCommit.getTree();

        return PendDifferenceTask.analyzeDifferences(
            repository,
            fromTree,
            toTree,
            RenameMode.ALL,
            new NullTaskProgressMonitor());
    }
}
