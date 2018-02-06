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

package com.microsoft.gittf.core.upgrade;

import java.io.File;

import org.eclipse.jgit.lib.Repository;

import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.config.ChangesetCommitMap;
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.tfs.util.FileHelpers;

/**
 * Manages any upgrade needed in the config file format
 */
public class UpgradeManager
{
    /**
     * Upgrades the repository configuration if the current configuration
     * version is not the most recent version
     * 
     * @param repository
     *        the git repository
     * @throws Exception
     */
    public static void upgradeIfNeccessary(final Repository repository)
        throws Exception
    {
        /* Load the configuration */
        final GitTFConfiguration currentConfiguration = GitTFConfiguration.loadFrom(repository);

        /* if this repository is not configured exit */
        if (currentConfiguration == null)
        {
            return;
        }

        /* Load the current file format version */
        final int existingFormat = currentConfiguration.getFileFormatVersion();

        /* if the format version is up to date return */
        if (existingFormat == GitTFConstants.GIT_TF_CURRENT_FORMAT_VERSION)
        {
            return;
        }

        /* if the version is zero upgrade to version one */
        if (existingFormat == 0)
        {
            upgradeFromV0ToV1(repository, currentConfiguration);
        }
    }

    private static void upgradeFromV0ToV1(final Repository repository, final GitTFConfiguration currentConfiguration)
        throws Exception
    {
        /*
         * Delete old temp git-tf folder sense we will create a config with the
         * same name. Newly created temp git-tf folder will be named "tf".
         */
        final File currentTempFileLocation = new File(repository.getDirectory(), GitTFConstants.GIT_TF_NAME);
        if (currentTempFileLocation.exists() && currentTempFileLocation.isDirectory())
        {
            final boolean cleanupTempDir = FileHelpers.deleteDirectory(currentTempFileLocation);
            if (!cleanupTempDir)
            {
                throw new Exception(
                    Messages.formatString(
                        "UpgradeManager.upgradeFromV0ToV1.CannotCleanUpTempDirectoryFormat", currentTempFileLocation.getAbsolutePath())); //$NON-NLS-1$
            }
        }

        /*
         * Move the existing "changesets" and "commits" sections to the new
         * "git-tf" config file
         */
        final File newConfigFileLocation = new File(repository.getDirectory(), GitTFConstants.GIT_TF_NAME);
        if (!newConfigFileLocation.exists())
        {
            ChangesetCommitMap.copyConfigurationEntriesFromRepositoryConfigToNewConfig(
                repository,
                newConfigFileLocation);
        }

        currentConfiguration.setFileFormatVersion(1);
        currentConfiguration.saveTo(repository);
    }
}
