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

package com.microsoft.gittf.client.tfs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;

import com.microsoft.gittf.client.tfs.Library.GitCommand;
import com.microsoft.gittf.client.tfs.Library.GitTfCommand;
import com.microsoft.gittf.client.tfs.Library.InvalidConfigurationException;
import com.microsoft.gittf.client.tfs.Library.Logger;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

/**
 * Class: TfsCheckinTest Description: This class will contain the git-tf tests
 * which checkin to TFS.
 * 
 * @author jpresto
 * 
 */
public class TfsEndToEndTests
    extends GitTfTestBase
{
    /**
     * Here, we keep a mapping for the content and the file we have; this will
     * allow us to verify the content in different locations when needed.
     */
    private Hashtable<File, String> m_fileContent = new Hashtable<File, String>();

    /**
     * Test: Tfs checkin Steps: 1. Clone root of the tfs repository with team
     * project named 'GitTesting' 2. Add a file to the team project [git add] 3.
     * Commit file to git [git commit] 4. Sync to TFS [git tf checkin] 5. Edit
     * the same file [git add] 6. Commit to git [git commit] 7. Sync to TFS 8.
     * Clone root of the tfs repository again to a different folder 9. Verify
     * files and content of files in new folder 10. Using TFS, branch the root
     * of the working folder 11. Kick off a pull request 12. Verify that the
     * branch is pulled down
     */
    public void testGitTfEndToEnd()
    {
        if (!configured)
        {
            return;
        }

        try
        {
            // scenarioBranchFromTfs();

            // 1. Clone root of the tfs repository
            stepGitTfEndToEndClone();

            // 2. Add a file to the team project [git add]
            stepAddFileToGitRepo();

            // 3. Commit file to git [git commit]
            stepCommitChangesToGit();

            // 4. Sync to TFS [git tf checkin]
            stepSyncChangesToTfs();

            // 5. Edit the same file [git add]
            stepEditFileAddToGit();

            // 6. Commit to git [git commit]
            stepCommitChangesToGit();

            // 7. Sync to TFS
            stepSyncChangesToTfs();

            // 8. Rename file [git mv]
            stepRenameFileInGit();

            // 9. Add file to git [git add]
            stepAddFileToGitRepo();

            // 10. Commit to git [git commit]
            stepCommitChangesToGit();

            // 11. Checkin to TFS
            stepSyncChangesToTfs();

            // 12. Clone root of the tfs repository again to a different
            getWorkspaceFolder(true);
            stepGitTfEndToEndClone();

            // 13. Verify files and content of files in new folder
            stepVerifyFilesInNewRepo();

            // Step: Branch working folder in TFS
            stepBranchFromTfs();

            // Step: Kick off pull request from git
            stepPullFromTfs();

            // Step: Verify branch is pulled down
            stepVerifyFilesInNewRepo();
        }
        catch (Throwable e2)
        {
            Logger.log("Unexpected exception during test"); //$NON-NLS-1$
            Logger.logException(e2);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$  
        }
    }

    /**
     * Clone a TFS repository.
     */
    private void stepGitTfEndToEndClone()
        throws InvalidConfigurationException
    {
        String scenarioDescription = "Cloning TFS repository"; //$NON-NLS-1$
        Logger.logHeader(MessageFormat.format("Step: {0}", scenarioDescription)); //$NON-NLS-1$

        // RUN: kick off the command
        try
        {
            Logger.log(MessageFormat.format("Running: {0}", scenarioDescription)); //$NON-NLS-1$
            GitTfCommand cmd =
                new GitTfCommand(MessageFormat.format("clone {0} $/", TestEnvironment.getCollectionUrl())); //$NON-NLS-1$
            cmd.getWorkingFolder(getWorkspaceFolder());
            cmd.run();

            cmd.logResults();

            assertEquals(0, cmd.getExitCode());
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'testGitTfEndToEndClone' (RUN)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }

        // VERIFY: verify we have the correct folder (team project) retrieved
        try
        {
            Logger.log(MessageFormat.format("Verifying: {0}", scenarioDescription)); //$NON-NLS-1$
            Logger.log("Iterating through the folder looking for the team project"); //$NON-NLS-1$
            File folder = new File(getWorkspaceFolder());

            File[] folderChildren = folder.listFiles();

            if (folderChildren == null)
            {
                throw new InvalidConfigurationException(
                    "There are not folders (Team Projects) at the root of the collection"); //$NON-NLS-1$
            }

            // check for the team project name
            Boolean teamProjectFound = false;
            for (int i = 0; i < folderChildren.length; i++)
            {
                if (folderChildren[i].isFile())
                {
                    continue;
                }

                if (folderChildren[i].getName().equals(TestEnvironment.getTfsTeamProjectName()))
                {
                    Logger.log(MessageFormat.format(
                        "Found folder/team project named: {0}", TestEnvironment.getTfsTeamProjectName())); //$NON-NLS-1$
                    teamProjectFound = true;
                    teamProjectLocalFile = folderChildren[i];
                    break;
                }
            }

            if (!teamProjectFound)
            {
                throw new InvalidConfigurationException(
                    MessageFormat.format(
                        "Team Project '{0}' was not found after the clone; be sure that team project already is created and the current user has perission to that collection and team project", TestEnvironment.getTfsTeamProjectName())); //$NON-NLS-1$
            }
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'testGitTfEndToEndClone' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }
    }

    /**
     * Kick off the command to add a file to git.
     */
    private void stepAddFileToGitRepo()
    {
        GitCommand gitCommand = null;

        String scenarioDescription = "Adding file to Git repo"; //$NON-NLS-1$
        Logger.logHeader(MessageFormat.format("Step: {0}", scenarioDescription)); //$NON-NLS-1$

        // RUN: kick off the command
        try
        {
            Logger.log(MessageFormat.format("Running: {0}", scenarioDescription)); //$NON-NLS-1$

            // create file
            if (currentWorkingFolder == null || currentWorkingFolder.length() == 0)
            {
                currentWorkingFolder = createFolderInWorkspace();
            }
            File newFile = createTimeStampedFile(new File(currentWorkingFolder));

            // put some content in the file
            BufferedWriter out = new BufferedWriter(new FileWriter(newFile));
            String content = getRandomFileContent(10);
            out.write(content);
            out.close();

            m_fileContent.put(newFile, content);

            // add the file to git
            gitCommand = new GitCommand(MessageFormat.format("add {0}", newFile.getAbsolutePath())); //$NON-NLS-1$
            gitCommand.getWorkingFolder(currentWorkingFolder);
            gitCommand.runCommand();

            gitCommand.logResults();
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'scenarioAddFileToGitRepo' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }

        // VERIFY: kick off the command
        try
        {
            Logger.log(MessageFormat.format("Verifying: {0}", scenarioDescription)); //$NON-NLS-1$

            assertNotNull(gitCommand);

            assertEquals(0, gitCommand.getExitCode());

            // for logging purposes, lets do a quick status
            gitCommand = new GitCommand("status"); //$NON-NLS-1$
            gitCommand.getWorkingFolder(currentWorkingFolder);
            gitCommand.runCommand();

            gitCommand.logResults();
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'scenarioAddFileToGitRepo' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }
    }

    /**
     * Kick off commit command for git.
     */
    private void stepCommitChangesToGit()
    {
        GitCommand gitCommand = null;

        String scenarioDescription = "Commit file to git [git commit]"; //$NON-NLS-1$
        Logger.logHeader(MessageFormat.format("Step: {0}", scenarioDescription)); //$NON-NLS-1$

        // RUN: kick off the command
        try
        {
            Logger.log(MessageFormat.format("Running: {0}", scenarioDescription)); //$NON-NLS-1$

            // create file
            File newFile = createTimeStampedFile(teamProjectLocalFile);

            // put some content in the file
            BufferedWriter out = new BufferedWriter(new FileWriter(newFile));
            String content = getRandomFileContent(10);
            out.write(content);
            out.close();

            // add the file to git
            gitCommand = new GitCommand("commit -m\"test\""); //$NON-NLS-1$
            gitCommand.getWorkingFolder(currentWorkingFolder);
            gitCommand.runCommand();

            gitCommand.logResults();
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'scenarioCommitChangesToGit' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }

        // VERIFY: kick off the command
        try
        {
            Logger.log(MessageFormat.format("Verifying: {0}", scenarioDescription)); //$NON-NLS-1$

            assertNotNull(gitCommand);

            assertEquals(0, gitCommand.getExitCode());
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'scenarioCommitChangesToGit' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }
    }

    /**
     * Kick of the checkin to tfs.
     */
    private void stepSyncChangesToTfs()
    {
        String scenarioDescription = "Sync to TFS [git tf checkin]"; //$NON-NLS-1$
        Logger.logHeader(MessageFormat.format("Step: {0}", scenarioDescription)); //$NON-NLS-1$

        // RUN: kick off the command
        try
        {
            Logger.log(MessageFormat.format("Running: {0}", scenarioDescription)); //$NON-NLS-1$ 

            // do a pull first
            Logger.log("Kicking off a pull --rebase before the checkin"); //$NON-NLS-1$
            GitTfCommand cmd = new GitTfCommand("pull --rebase"); //$NON-NLS-1$
            cmd.getWorkingFolder(currentWorkingFolder);
            cmd.run();

            Logger.log("Running the checkin"); //$NON-NLS-1$
            cmd = new GitTfCommand("checkin"); //$NON-NLS-1$
            cmd.getWorkingFolder(currentWorkingFolder);
            cmd.run();

            cmd.logResults();

            if (cmd.getExitCode() != 0)
            {
                cmd.logDetails();
            }

            assertEquals(0, cmd.getExitCode());
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'testGitTfEndToEndClone' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }

        // VERIFY: kick off the command
        try
        {
            Logger.log(MessageFormat.format("Verifying {0}", scenarioDescription)); //$NON-NLS-1$
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'testGitTfEndToEndClone' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }
    }

    /**
     * Kick off the edit file and add to git.
     */
    private void stepEditFileAddToGit()
    {
        GitCommand gitCommand = null;

        String scenarioDescription = "Edit the same file [git add]"; //$NON-NLS-1$
        Logger.logHeader(MessageFormat.format("Step: {0}", scenarioDescription)); //$NON-NLS-1$

        // RUN: kick off the command
        try
        {
            Logger.log(MessageFormat.format("Running: {0}", scenarioDescription)); //$NON-NLS-1$

            // edit file all files
            Enumeration<File> iterator = m_fileContent.keys();
            while (iterator.hasMoreElements())
            {
                // get the file object
                File file = iterator.nextElement();

                // change the content, save, and save the new content
                BufferedWriter out = new BufferedWriter(new FileWriter(file));
                String content = getRandomFileContent(20);
                out.write(content);
                out.close();

                gitCommand = new GitCommand(MessageFormat.format("add {0}", file.getAbsolutePath())); //$NON-NLS-1$
                gitCommand.getWorkingFolder(currentWorkingFolder);
                gitCommand.runCommand();

                gitCommand.logResults();

                m_fileContent.put(file, content);
            }
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'scenarioEditFileAddToGit' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }
    }

    /**
     * Verify the content and names of the files in the replicated workspace.
     */
    private void stepVerifyFilesInNewRepo()
    {
        BufferedReader reader = null;

        try
        {
            // iterate through each file and verify the content in the new
            // location
            gitRepositoryRootFolder2 = getWorkspaceFolder();

            // edit file all files
            Enumeration<File> iterator = m_fileContent.keys();
            while (iterator.hasMoreElements())
            {
                // get the file object
                File file = iterator.nextElement();

                Logger.logHeader(MessageFormat.format("File: {0}", file.getAbsoluteFile())); //$NON-NLS-1$

                // open the file (remember that the items in this hastable are
                // paths
                // against the original folder - we need to replace that with
                // the
                // new folder
                File newFile = new File(file.getPath().replace(gitRepositoryRootFolder1, gitRepositoryRootFolder2));
                Logger.log(MessageFormat.format("Processing file: {0}", newFile.getPath())); //$NON-NLS-1$

                String actualContent = ""; //$NON-NLS-1$
                String newLineOfActualContent = null;
                reader = new BufferedReader(new FileReader(newFile));
                while ((newLineOfActualContent = reader.readLine()) != null)
                {
                    actualContent = actualContent + newLineOfActualContent + TestEnvironmentConstants.GetNewLine();
                }

                String expectedContent = m_fileContent.get(file);
                Logger.log("Verify the content of the new file in the new location."); //$NON-NLS-1$
                Logger.log("Actual Content", MessageFormat.format("{0}", actualContent.toString())); //$NON-NLS-1$ //$NON-NLS-2$
                Logger.log("Expected Content", MessageFormat.format("{0}", expectedContent.toString())); //$NON-NLS-1$ //$NON-NLS-2$
                assertEquals(expectedContent.toString(), actualContent.toString());
            }
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'scenarioVerifyFilesInNewRepo' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$           
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    // best attempt
                }
            }
        }
    }

    /**
     * Rename file in git.
     */
    private void stepRenameFileInGit()
    {
        String scenarioDescription = "Rename file [git mv]"; //$NON-NLS-1$
        Logger.logHeader(MessageFormat.format("Step: {0}", scenarioDescription)); //$NON-NLS-1$

        // RUN: kick off the command
        try
        {
            Logger.log(MessageFormat.format("Running: {0}", scenarioDescription)); //$NON-NLS-1$ 

            // get the file to update
            File file = m_fileContent.keys().nextElement();
            String content = m_fileContent.get(file);
            m_fileContent.clear();

            File newFile = new File(MessageFormat.format("{0}.new", file.getAbsolutePath())); //$NON-NLS-1$
            m_fileContent.put(newFile, content);

            // add the file to git
            GitCommand gitCommand = null;

            gitCommand =
                new GitCommand(MessageFormat.format("mv {0} {1}", file.getAbsolutePath(), newFile.getAbsolutePath())); //$NON-NLS-1$
            gitCommand.getWorkingFolder(currentWorkingFolder);
            gitCommand.runCommand();

            // save the new object
            gitCommand.logResults();
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'scenarioEditFileAddToGit' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }
    }

    /**
     * Branch the working folder from the TFS object model (action not triggered
     * from git.
     */
    private void stepBranchFromTfs()
    {
        String scenarioDescription = "Branch using TFS OM"; //$NON-NLS-1$
        Logger.logHeader(MessageFormat.format("Step: {0}", scenarioDescription)); //$NON-NLS-1$
        String newBranchName = null;
        File file = null;

        try
        {
            Logger.log(MessageFormat.format("Running: {0}", scenarioDescription)); //$NON-NLS-1$ 

            TFSTeamProjectCollection tfs = connectToTFS();

            VersionControlClient vcClient = tfs.getVersionControlClient();

            VersionSpec version = LatestVersionSpec.INSTANCE;

            file = new File(currentWorkingFolder);
            newBranchName = MessageFormat.format("{0}-branch", file.getName()); //$NON-NLS-1$
            String currentServerpath =
                ServerPath.combine(ServerPath.combine("$/", TestEnvironment.getTfsTeamProjectName()), //$NON-NLS-1$
                    file.getName());
            String newBranchServerPath =
                ServerPath.combine(ServerPath.combine("$/", TestEnvironment.getTfsTeamProjectName()), //$NON-NLS-1$
                    newBranchName);

            int returnCode = vcClient.createBranch(currentServerpath, newBranchServerPath, version);

            Logger.log(MessageFormat.format("Return Code: {0}", returnCode)); //$NON-NLS-1$

            // update the list of files with the new file
            Enumeration<File> iterator = m_fileContent.keys();
            while (iterator.hasMoreElements())
            {
                // get the file object
                File existingFile = iterator.nextElement();
                String escapedPath =
                    existingFile.getPath().replace(
                        existingFile.getParent(),
                        MessageFormat.format("{0}-branch", file.getPath())); //$NON-NLS-1$

                File newFile = new File(escapedPath);

                m_fileContent.put(newFile, m_fileContent.get(existingFile));
            }

            // wait a few seconds due to time stamp issues
            Thread.sleep(5 * 1000);
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'scenarioBranchFromTfs' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }
    }

    /**
     * Pull from TFS (this will be the branch in this tests case).
     */
    private void stepPullFromTfs()
    {
        String scenarioDescription = "Pull fro TFS"; //$NON-NLS-1$
        Logger.logHeader(MessageFormat.format("Step: {0}", scenarioDescription)); //$NON-NLS-1$

        try
        {
            Logger.log(MessageFormat.format("Running: {0}", scenarioDescription)); //$NON-NLS-1$ 

            GitTfCommand cmd = new GitTfCommand("pull --rebase"); //$NON-NLS-1$
            cmd.getWorkingFolder(getWorkspaceFolder());
            cmd.run();

            // save the new object
            cmd.logResults();
        }
        catch (Throwable e)
        {
            Logger.log("Unexpected exception during 'scenarioPullFromTfs' (VERIFY)"); //$NON-NLS-1$
            Logger.logException(e);
            super.fail("Clone of TFS failed"); //$NON-NLS-1$
        }
    }
}
