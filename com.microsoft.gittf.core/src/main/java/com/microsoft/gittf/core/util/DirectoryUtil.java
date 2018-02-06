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

package com.microsoft.gittf.core.util;

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;

import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.GUID;

public final class DirectoryUtil
{
    public static final String TEMP_DIR_NAME = "temp"; //$NON-NLS-1$

    private DirectoryUtil()
    {
    }

    /**
     * Get the directory to use for staging changes to TFS.
     * 
     * @param repository
     *        the git repository
     * @return
     */
    public static File getTempDirRoot(final Repository repository)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        GitTFConfiguration config = GitTFConfiguration.loadFrom(repository);
        if (config.getTempDirectory() != null)
        {
            return new File(config.getTempDirectory());
        }

        File rootDirectory = repository.getDirectory().getAbsoluteFile();

        try
        {
            rootDirectory = rootDirectory.getCanonicalFile();
        }
        catch (IOException e)
        {
            /* suppress */
        }

        return new File(rootDirectory, GitTFConstants.GIT_TF_DIRNAME);
    }

    /**
     * Get the temp directory that should be used in the git repository
     * 
     * @param repository
     *        the git repository
     * @return
     */
    public static File getTempDir(final Repository repository)
    {
        return getTempDir(repository, GUID.newGUID());
    }

    private static File getTempDir(final Repository repository, GUID id)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(id, "id"); //$NON-NLS-1$

        int count = 0;
        while (count < 5)
        {
            File possibleTempFolder =
                new File(getTempDirRoot(repository), getUniqueAbbreviatedFolderName(repository, GUID.newGUID()));
            if (!possibleTempFolder.exists())
            {
                return possibleTempFolder;
            }
        }

        return new File(getTempDirRoot(repository), MessageFormat.format("{0}-{1}", TEMP_DIR_NAME, id.getGUIDString())); //$NON-NLS-1$
    }

    private static String getUniqueAbbreviatedFolderName(final Repository repository, GUID guid)
    {
        ObjectInserter objIns = null;
        try
        {
            objIns = repository.newObjectInserter();

            return ObjectIdUtil.abbreviate(repository, objIns.idFor(OBJ_BLOB, guid.getGUIDBytes()));
        }
        catch (Exception e)
        {
            return guid.getGUIDString();
        }
        finally
        {
            if (objIns != null)
            {
                objIns.release();
            }
        }

    }

    /**
     * Creates a directory and returns the File object of the first created
     * parent
     * 
     * @param location
     * @return
     */
    public static File createDirectory(File location)
    {
        boolean canCreateDirectory = true;
        File toReturn = null;

        if (!location.getParentFile().exists())
        {
            toReturn = createDirectory(location.getParentFile());
            canCreateDirectory = toReturn != null;
        }
        else
        {
            toReturn = location;
        }

        if (canCreateDirectory)
        {
            if (!location.mkdir())
            {
                FileHelpers.deleteDirectory(toReturn);
            }
            else
            {
                return toReturn;
            }
        }

        return null;
    }
}
