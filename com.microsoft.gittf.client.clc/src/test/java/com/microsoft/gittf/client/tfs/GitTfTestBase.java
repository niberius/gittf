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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Random;

import junit.framework.TestCase;

import com.microsoft.gittf.client.tfs.Library.GitCommand;
import com.microsoft.gittf.client.tfs.Library.Logger;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.util.URIUtils;

/**
 * 
 * @author jpresto
 * 
 *         Base class for the tests. This class will initialization and basic
 *         methods tests will use.
 * 
 */
public class GitTfTestBase
    extends TestCase
{
    private String runFolder = null;
    private String defaultWorkspaceFolder = null;
    private String teamProjectFolder = null;
    protected File teamProjectLocalFile = null;
    protected String gitRepositoryRootFolder1 = null;
    protected String gitRepositoryRootFolder2 = null;
    protected String currentWorkingFolder = null;
    protected Boolean configured = false;

    /**
     * Setup for each test.
     */
    protected void setUp()
    {
        Logger.logHeader("Starting test initialization: 'setUp'"); //$NON-NLS-1$
        try
        {
            // if we are not configured to run tests, lets bail out
            configured = TestEnvironment.readConfiguration();

            if (!configured)
            {
                Logger.log("No configuration found for running this test..."); //$NON-NLS-1$
                return;
            }

            // create a local root path for original repository
            gitRepositoryRootFolder1 = getWorkspaceFolder();

            // configure git with user
            // TODO setupDefaultUser();
        }
        catch (Throwable e)
        {
            fail(e.getMessage());
        }
    }

    /**
     * Cleanup.
     */
    protected void tearDown()
    {
        File runFolder = new File(this.getRunFolder());

        deleteFolder(runFolder);
    }

    /**
     * Best effort delete of run folders.
     * 
     * @param fileOrFolder
     */
    private Boolean deleteFolder(File fileOrFolder)
    {
        if (fileOrFolder.isDirectory())
        {
            File[] files = fileOrFolder.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                File nextFile = files[i];
                if (nextFile.isDirectory())
                {
                    deleteFolder(nextFile);
                }
                else
                {
                    nextFile.delete();
                }
            }
        }
        return fileOrFolder.delete();
    }

    /**
     * File structure for tests: <root>\RUN_123\WS_123\<team
     * project>\FOLDER_123\File_123.txt
     * 
     * @param rootPath
     *        - the root path where the child folder will exist
     * @param prefix
     *        - option prefix to the folder
     * @return full path of the new file
     */
    private String createFolder(String rootPath, String prefix)
    {
        // create the new folder
        File folder = new File(rootPath);
        File childFolder = new File(folder, getTimeStampFileName(prefix, null));

        if (!childFolder.mkdirs())
        {
            // failed
            return null;
        }

        // create the folder
        childFolder.mkdirs();

        return childFolder.getPath();
    }

    /**
     * Create the folder (top level) for this run and return it.
     * 
     * @return local folder where the test for this run will be stored
     */
    public String getRunFolder()
    {
        if (runFolder == null)
        {
            runFolder = createFolder(TestEnvironment.getGitRepositoryRootPath(), "RUN_"); //$NON-NLS-1$
            Logger.log(MessageFormat.format("Create run folder: {0}", runFolder)); //$NON-NLS-1$
        }

        return runFolder;
    }

    /**
     * 
     * @return the team project local folder
     */
    public String getTeamProjectFolder()
    {
        if (teamProjectFolder == null)
        {
            File workspaceFolder = new File(getWorkspaceFolder());
            File teamProjectFileFolder = new File(workspaceFolder, TestEnvironment.getTfsTeamProjectName());
            teamProjectFolder = teamProjectFileFolder.getPath();
            Logger.log(MessageFormat.format("Built team project folder: {0}", teamProjectFolder)); //$NON-NLS-1$
        }

        return teamProjectFolder;
    }

    /**
     * Get the current workspace folder (optionally create it).
     * 
     * @param forceNew
     *        - if true, the current workspace will be removed and a brand new
     *        workspace will be created
     * @return local path of this new workspace folder
     */
    public String getWorkspaceFolder(Boolean forceNew)
    {
        if ((defaultWorkspaceFolder == null) || (forceNew))
        {
            defaultWorkspaceFolder = createFolder(getRunFolder(), "WS_"); //$NON-NLS-1$
            Logger.log(MessageFormat.format("Create workspace folder: {0}", defaultWorkspaceFolder)); //$NON-NLS-1$
        }

        return defaultWorkspaceFolder;
    }

    /**
     * Get the current workspace folder; this will create it if it is not
     * already created.
     * 
     * @return
     */
    public String getWorkspaceFolder()
    {
        return getWorkspaceFolder(false);
    }

    /**
     * Set the current workspace folder.
     * 
     * @param workingFolder
     *        - local folder to use as the workspace folder
     */
    protected void setWorkspaceFolder(String workingFolder)
    {
        defaultWorkspaceFolder = workingFolder;
    }

    /**
     * Create a folder in the current workspace folder.
     * 
     * @return path to the newly created folder
     */
    public String createFolderInWorkspace()
    {
        return createFolder(getTeamProjectFolder(), "FOLDER_"); //$NON-NLS-1$
    }

    /**
     * Create a file with the timestamp in the name.
     * 
     * @param parent
     *        folder where the file should be created
     * @return the file which was created
     */
    protected File createTimeStampedFile(File parent)
    {
        File folder = new File(parent.getPath());
        File fileAndFolder = new File(folder, getTimeStampFileName("File_", ".txt")); //$NON-NLS-1$ //$NON-NLS-2$

        try
        {
            if (!fileAndFolder.createNewFile())
            {
                return null;
            }
        }
        catch (IOException e)
        {
            Logger.logException(e);
            return null;
        }

        return fileAndFolder;
    }

    /**
     * Get a name of a valid file (this DOES NOT create the file, it only
     * generates the name).
     * 
     * @return valid name of a file
     */
    protected String getTimeStampFileName()
    {
        return getTimeStampFileName(null, null);
    }

    /**
     * Get a name of a valid file optionally supplying a suffix and/or a prefix
     * for the name (this DOES NOT create the file, it only generates the name).
     * 
     * @param prefix
     *        optional prefix for the file
     * @param suffix
     *        optional suffix for the file
     * @return name of a valid file
     */
    protected String getTimeStampFileName(String prefix, String suffix)
    {
        DecimalFormat decimalFormat = new DecimalFormat("00"); //$NON-NLS-1$

        Calendar cal = Calendar.getInstance();
        String fileName = MessageFormat.format("{0}-{1}-{2}__{3}-{4}-{5}.{6}", //$NON-NLS-1$
            cal.get(Calendar.YEAR),
            decimalFormat.format(cal.get(Calendar.MONTH)),
            decimalFormat.format(cal.get(Calendar.DAY_OF_MONTH) + 1), // +1 -
                                                                      // 0-based
            decimalFormat.format(cal.get(Calendar.HOUR_OF_DAY)),
            decimalFormat.format(cal.get(Calendar.MINUTE)),
            decimalFormat.format(cal.get(Calendar.SECOND)),
            decimalFormat.format(cal.get(Calendar.MILLISECOND)));

        if (prefix != null)
        {
            fileName = prefix + fileName;
        }

        if (suffix != null)
        {
            fileName = fileName + suffix;
        }

        return fileName;
    }

    /**
     * String containing characters we want to choose from for generating random
     * strings.
     */
    private final static String VALIDCONTENTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"; //$NON-NLS-1$

    /**
     * Simulate some random file content; each line will be a string between a
     * min value in length and a max value and will end with a newline.
     * 
     * @param numberOfLines
     *        number of lines the file should contain
     * @return contents of the file content
     */
    protected String getRandomFileContent(int numberOfLines)
    {
        String content = ""; //$NON-NLS-1$
        Random random = new Random();

        for (int i = 0; i < numberOfLines; i++)
        {
            // each line will be, by default, between 0 and 50 characters long
            int newLineLength = random.nextInt(50);
            for (int j = 0; j < newLineLength; j++)
            {
                content = content + VALIDCONTENTCHARS.charAt(random.nextInt(VALIDCONTENTCHARS.length() - 1));
            }

            content = content + TestEnvironmentConstants.GetNewLine();
        }

        return content;
    }

    /**
     * Set the default user for git; this is not absolutely necessary, but will
     * minimize the output when git runs (logs will be easier to follow).
     */
    protected void setupDefaultUser()
    {
        // for logging purposes, lets do a quick status
        try
        {
            GitCommand gitCommand = null;
            gitCommand = new GitCommand("config --global user.name \"Test User\""); //$NON-NLS-1$
            gitCommand.getWorkingFolder(getWorkspaceFolder());
            gitCommand.runCommand();

            gitCommand = new GitCommand("config --global user.email you@example.com"); //$NON-NLS-1$
            gitCommand.getWorkingFolder(getWorkspaceFolder());
            gitCommand.runCommand();
        }
        catch (Exception e)
        {
            // ignore the config... if it fails, then we may get additional
            // information in the output of git.exe
        }
    }

    /**
     * Get the collection based on the input from the test environment file.
     * 
     * @return connection to TFS which has already been authenticated
     */
    public TFSTeamProjectCollection connectToTFS()
    {
        TFSTeamProjectCollection tpc = null;

        // use default credentials for now - TODO - this will have to change to
        // basic authentication credentials (option) for accessing
        // tfspreview.com for testing
        Credentials credentials = new DefaultNTCredentials();

        String url = TestEnvironment.getCollectionUrl();
        URI uri = URIUtils.newURI(url);
        tpc = new TFSTeamProjectCollection(uri, credentials);

        try
        {
            tpc.authenticate();
        }
        catch (Exception e)
        {
            Logger.logException(e);
        }

        return tpc;
    }

    /**
     * Get the version control client object model based on the test environment
     * file.
     * 
     * @return get the version control model used to access TFS directly
     */
    public VersionControlClient getVerionControlModel()
    {
        return connectToTFS().getVersionControlClient();
    }
}
