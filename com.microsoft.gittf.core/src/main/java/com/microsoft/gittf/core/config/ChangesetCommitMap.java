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

package com.microsoft.gittf.core.config;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.TagUtil;

/**
 * The ChangesetCommitMap class maintains the mapping between changesets and
 * commits. It also maintains the HWM which is the latest changeset downloaded
 * from TFS. All this information is stored in the .git\git-tf file in the
 * repository. This file uses the same format used by the config files.
 * 
 */
public class ChangesetCommitMap
{
    private final Repository repository;
    private final FileBasedConfig configFile;

    /**
     * Constructor
     * 
     * @param repository
     *        the git repository
     */
    public ChangesetCommitMap(final Repository repository)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.configFile =
            new FileBasedConfig(new File(repository.getDirectory(), GitTFConstants.GIT_TF_NAME), FS.DETECTED);
    }

    /**
     * Sets the commit id that this changeset refers to
     * 
     * @param changesetID
     *        the changeset id
     * @param commitID
     *        the commit id
     * @throws IOException
     */
    public void setChangesetCommit(int changesetID, ObjectId commitID)
        throws IOException
    {
        setChangesetCommit(changesetID, commitID, false);
    }

    public void setChangesetCommit(int changesetID, ObjectId commitID, boolean forceHWMUpdate)
        throws IOException
    {
        Check.isTrue(changesetID >= 0, "changesetID >= 0"); //$NON-NLS-1$
        Check.notNull(commitID, "commitID"); //$NON-NLS-1$

        ensureConfigUptoDate();

        cleanupPreviousEntries(changesetID);

        configFile.setString(
            ConfigurationConstants.CONFIGURATION_SECTION,
            ConfigurationConstants.COMMIT_SUBSECTION,
            MessageFormat.format(ConfigurationConstants.COMMIT_CHANGESET_FORMAT, Integer.toString(changesetID)),
            commitID.getName());

        configFile.setInt(
            ConfigurationConstants.CONFIGURATION_SECTION,
            ConfigurationConstants.CHANGESET_SUBSECTION,
            MessageFormat.format(ConfigurationConstants.CHANGESET_COMMIT_FORMAT, commitID.getName()),
            changesetID);

        /* Update the high water mark automatically */
        if ((changesetID > getLastBridgedChangesetID(false)) || forceHWMUpdate)
        {
            configFile.setInt(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.CHANGESET_SUBSECTION,
                ConfigurationConstants.CHANGESET_HIGHWATER,
                changesetID);
        }

        configFile.save();

        TagUtil.createTFSChangesetTag(repository, commitID, changesetID);
    }

    /**
     * Gets the changeset id that this commit refers to
     * 
     * @param commitID
     *        the commit id
     * @return
     */
    public int getChangesetID(ObjectId commitID)
    {
        Check.notNull(commitID, "commitID"); //$NON-NLS-1$

        ensureConfigUptoDate();

        return configFile.getInt(
            ConfigurationConstants.CONFIGURATION_SECTION,
            ConfigurationConstants.CHANGESET_SUBSECTION,
            MessageFormat.format(ConfigurationConstants.CHANGESET_COMMIT_FORMAT, commitID.getName()),
            -1);
    }

    /**
     * Gets the commit id that this changeset id refers to. If the caller
     * specified the validate flag as true, the method also ensures that the
     * commit id refers to a valid commit. If not NULL is returned.
     * 
     * @param changesetID
     *        the changeset id
     * @param validate
     *        whether or not to validate the object id found
     * @return
     */
    public ObjectId getCommitID(int changesetID, boolean validate)
    {
        Check.isTrue(changesetID >= 0, "changesetID >= 0"); //$NON-NLS-1$

        ensureConfigUptoDate();

        String commitHash =
            configFile.getString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.COMMIT_SUBSECTION,
                MessageFormat.format(ConfigurationConstants.COMMIT_CHANGESET_FORMAT, Integer.toString(changesetID)));

        if (commitHash == null)
        {
            return null;
        }

        ObjectId changesetCommitId = ObjectId.fromString(commitHash);

        if (!validate)
        {
            return changesetCommitId;
        }

        ObjectReader objectReader = null;
        try
        {
            objectReader = repository.newObjectReader();
            if (changesetCommitId != null && !ObjectId.zeroId().equals(changesetCommitId))
            {
                try
                {
                    if (objectReader.has(changesetCommitId))
                    {
                        return changesetCommitId;
                    }
                }
                catch (IOException exception)
                {
                    return null;
                }
            }

            return null;
        }
        finally
        {
            if (objectReader != null)
            {
                objectReader.release();
            }
        }
    }

    /**
     * Gets the last downloaded changeset id. If validate is specified, the
     * method will test the changeset id / commit id for existence, if the HWM
     * in the config file does not exist in the repository, the method will loop
     * through all the downloaded changesets to find the latest valid changeset
     * downloaded.
     * 
     * @param validate
     *        whether or not to validate the HWM value
     * @return
     */
    public int getLastBridgedChangesetID(boolean validate)
    {
        ensureConfigUptoDate();

        int changeset =
            configFile.getInt(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.CHANGESET_SUBSECTION,
                ConfigurationConstants.CHANGESET_HIGHWATER,
                -1);

        if (changeset < 0)
        {
            return changeset;
        }

        if (!validate)
        {
            return changeset;
        }

        while (true)
        {
            if (changeset < 0)
            {
                return changeset;
            }

            ObjectId changesetCommitId = getCommitID(changeset, true);

            if (changesetCommitId != null && !ObjectId.zeroId().equals(changesetCommitId))
            {
                return changeset;
            }

            changeset = getPreviousBridgedChangeset(changeset, false);
        }
    }

    /**
     * Gets the first changeset downloaded before the changeset specified.
     * 
     * @param changesetID
     * @param validate
     * @return
     */
    public int getPreviousBridgedChangeset(int changesetID, boolean validate)
    {
        ensureConfigUptoDate();

        Set<String> downloadedChangesetEntries =
            configFile.getNames(ConfigurationConstants.CONFIGURATION_SECTION, ConfigurationConstants.COMMIT_SUBSECTION);

        Set<Integer> sortedDownloadedChangesetEntries = new TreeSet<Integer>(Collections.reverseOrder());

        for (String downloadedChangesetEntry : downloadedChangesetEntries)
        {
            String changesetNumberString =
                downloadedChangesetEntry.substring(MessageFormat.format(
                    ConfigurationConstants.COMMIT_CHANGESET_FORMAT,
                    "").length()); //$NON-NLS-1$

            int changesetIDEntry = Integer.parseInt(changesetNumberString);

            sortedDownloadedChangesetEntries.add(changesetIDEntry);
        }

        Iterator<Integer> changesetIterator = sortedDownloadedChangesetEntries.iterator();
        while (changesetIterator.hasNext())
        {
            int currentChangeset = changesetIterator.next();

            if (currentChangeset >= changesetID)
            {
                continue;
            }

            if (!validate)
            {
                return currentChangeset;
            }
            else
            {
                ObjectId commitId = getCommitID(currentChangeset, true);
                if (commitId != null && !ObjectId.zeroId().equals(commitId))
                {
                    return currentChangeset;
                }
            }
        }

        return -1;
    }

    /**
     * Cleans the entries for the changeset specified
     * 
     * @param changesetID
     */
    private void cleanupPreviousEntries(int changesetID)
    {
        String commitHash =
            configFile.getString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.COMMIT_SUBSECTION,
                MessageFormat.format(ConfigurationConstants.COMMIT_CHANGESET_FORMAT, Integer.toString(changesetID)));

        if (commitHash == null || commitHash.length() == 0)
        {
            return;
        }

        configFile.unset(
            ConfigurationConstants.CONFIGURATION_SECTION,
            ConfigurationConstants.COMMIT_SUBSECTION,
            MessageFormat.format(ConfigurationConstants.COMMIT_CHANGESET_FORMAT, Integer.toString(changesetID)));

        configFile.unset(
            ConfigurationConstants.CONFIGURATION_SECTION,
            ConfigurationConstants.CHANGESET_SUBSECTION,
            MessageFormat.format(ConfigurationConstants.CHANGESET_COMMIT_FORMAT, commitHash));
    }

    /**
     * Ensures that the config cache is up to date before reading it
     */
    private void ensureConfigUptoDate()
    {
        try
        {
            if (configFile != null && configFile.isOutdated())
            {
                configFile.load();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Used for upgrade, copoes the entries from repository config to the new
     * config location
     * 
     * @param newConfigLocation
     * @param repository
     */
    public static void copyConfigurationEntriesFromRepositoryConfigToNewConfig(
        Repository repository,
        File newConfigLocation)
    {
        if (!newConfigLocation.exists())
        {
            FileBasedConfig configFile = new FileBasedConfig(newConfigLocation, FS.DETECTED);

            GitTFConfiguration repositoryConfiguration = GitTFConfiguration.loadFrom(repository);
            if (repositoryConfiguration != null)
            {
                copyConfigurationEntries(repository.getConfig(), configFile);
            }
        }
    }

    private static void copyConfigurationEntries(StoredConfig source, FileBasedConfig target)
    {
        Set<String> downloadedChangesetEntries =
            source.getNames(ConfigurationConstants.CONFIGURATION_SECTION, ConfigurationConstants.COMMIT_SUBSECTION);

        for (String changesetEntry : downloadedChangesetEntries)
        {
            String commitHash =
                source.getString(
                    ConfigurationConstants.CONFIGURATION_SECTION,
                    ConfigurationConstants.COMMIT_SUBSECTION,
                    changesetEntry);

            target.setString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.COMMIT_SUBSECTION,
                changesetEntry,
                commitHash);
        }

        Set<String> createdCommitEntries =
            source.getNames(ConfigurationConstants.CONFIGURATION_SECTION, ConfigurationConstants.CHANGESET_SUBSECTION);

        for (String commitEntry : createdCommitEntries)
        {
            int changesetId =
                source.getInt(
                    ConfigurationConstants.CONFIGURATION_SECTION,
                    ConfigurationConstants.CHANGESET_SUBSECTION,
                    commitEntry,
                    -1);

            target.setInt(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.CHANGESET_SUBSECTION,
                commitEntry,
                changesetId);
        }

        source.unsetSection(ConfigurationConstants.CONFIGURATION_SECTION, ConfigurationConstants.CHANGESET_SUBSECTION);
        source.unsetSection(ConfigurationConstants.CONFIGURATION_SECTION, ConfigurationConstants.COMMIT_SUBSECTION);

        try
        {
            target.save();
            source.save();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
