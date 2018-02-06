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

package com.microsoft.gittf.core.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

public class Util
{
    private static final String GIT_REPO_LOCATION = "git-repo"; //$NON-NLS-1$
    private static final String TEST_CASES_LOCATION = "gitTftestCases"; //$NON-NLS-1$

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                cleanupDirectory(getTempFolder());
            }
        });
    }

    private Util()
    {

    }

    public static void setUp(String testCaseName)
        throws Exception
    {
        File testCaseTempLocation = getTemporaryTestFilesLocation(testCaseName);

        cleanupDirectory(testCaseTempLocation);

        testCaseTempLocation.mkdir();
    }

    public static void tearDown(String testCaseName)
        throws Exception
    {
        cleanupDirectory(getTemporaryTestFilesLocation(testCaseName));
    }

    public static Repository initializeGitRepo(String testCaseName)
        throws IOException
    {
        return new FileRepository(getRepositoryFile(testCaseName));
    }

    public static File getRepositoryFile(String testCaseName)
    {
        return new File(getTemporaryTestFilesLocation(testCaseName), GIT_REPO_LOCATION);
    }

    public static File getTemporaryTestFilesLocation(String testCaseName)
    {
        return new File(getTempFolder(), testCaseName);
    }

    public static String generateContentForFileInGit(String filePath)
    {
        return "Git : " + filePath; //$NON-NLS-1$
    }

    public static boolean verifyFileContent(File file, String expectedContent)
    {
        try
        {
            String fileContent = readFileIntoString(file);
            return fileContent.equals(expectedContent);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static File getTempFolder()
    {
        return new File(System.getProperty("java.io.tmpdir"), TEST_CASES_LOCATION); //$NON-NLS-1$
    }

    private static boolean cleanupDirectory(File path)
    {
        if (path.exists())
        {
            for (File file : path.listFiles())
            {
                if (file.isDirectory())
                {
                    cleanupDirectory(file);
                }
                else
                {
                    file.delete();
                }
            }
        }

        return (path.delete());
    }

    private static String readFileIntoString(File localFile)
        throws Exception
    {
        FileInputStream stream = new FileInputStream(localFile);
        try
        {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            /* Instead of using default, pass in a decoder. */
            return Charset.defaultCharset().decode(bb).toString();
        }
        finally
        {
            stream.close();
        }
    }

    public static void touchFile(File toTest)
        throws IOException
    {
        FileWriter fstream = new FileWriter(toTest, true);
        BufferedWriter out = new BufferedWriter(fstream);

        out.write("."); //$NON-NLS-1$
        out.close();
    }

    public static void move(File from, File to)
    {
        to.getParentFile().mkdirs();
        from.renameTo(to);
    }
}
