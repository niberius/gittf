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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.CoreConfig.AutoCRLF;
import org.eclipse.jgit.lib.Repository;

import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.OutputConstants;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.StringUtil;

/**
 * Configuration data for the git-tf command, read from the .git/config file.
 * 
 * Note: all data is saved in plain text to the git configuration, no attempt to
 * obscure (or otherwise poorly encrypt) potentially sensitive data (for
 * example, username or password) is provided. Clients must provide warning to
 * the user before saving sensitive information to the git repository and should
 * use other secure storage mechanisms (eg, DPAPI, Keychain, etc) whenever
 * possible.
 * 
 * @threadsafety unknown
 */
public class GitTFConfiguration
{
    private static final Log log = LogFactory.getLog(GitTFConfiguration.class);

    /* Server section parameters */
    private final URI serverURI;
    private final String tfsPath;
    private String username;
    private String password;
    private String buildDefinition;

    /* General section parameters */
    private boolean deep;
    private boolean tag;
    private boolean includeMetaData;
    private int fileFormatVersion;
    private String tempDirectory;
    private boolean keepAuthor;
    private String userMap;

    /* Parameter names defined in the local repository config file */
    private final Map<String, Boolean> locallyDefinedNames;

    /**
     * Creates a new git-tf configuration, suitable for use by the command.
     * 
     * @param serverURI
     *        The URI of the TFS server (must not be <code>null</code>)
     * @param tfsPath
     *        The server path that will be bridged to the git repository (must
     *        not be <code>null</code>)
     * @param username
     *        The username to connect to TFS as or <code>null</code> if no
     *        username should be saved
     * @param password
     *        The password to connect to TFS as or <code>null</code> if no
     *        password should be saved
     * @param depth
     *        The default "depth" for operations
     * @param includeMetaData
     *        The default setting for including metadata on changesets
     * @param tempDirectory
     *        The temporary directory to use
     * @param locallyDefinedNames
     *        Parameter names defined in the local repository config file (must
     *        not be <code>null</code>)
     */
    public GitTFConfiguration(
        final URI serverURI,
        final String tfsPath,
        final String username,
        final String password,
        final boolean deep,
        final boolean tag,
        final boolean includeMetaData,
        final int fileFormatVersion,
        final String buildDefinition,
        final String tempDirectory,
        final boolean keepAuthor,
        final String userMap,
        final Map<String, Boolean> locallyDefinedNames)
    {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNullOrEmpty(tfsPath, "tfsPath"); //$NON-NLS-1$
        Check.notNull(locallyDefinedNames, "definedNames"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.tfsPath = tfsPath;
        this.username = username;
        this.password = password;
        this.deep = deep;
        this.tag = tag;
        this.includeMetaData = includeMetaData;
        this.fileFormatVersion = fileFormatVersion;
        this.buildDefinition = buildDefinition;
        this.tempDirectory = tempDirectory;
        this.keepAuthor = keepAuthor;
        this.userMap = userMap;
        this.locallyDefinedNames = locallyDefinedNames;
    }

    /**
     * Creates a new git-tf configuration, suitable for writing to the
     * repository's git configuration.
     * 
     * @param serverURI
     *        The URI of the TFS server (must not be <code>null</code>)
     * @param tfsPath
     *        The server path that will be bridged to the git repository (must
     *        not be <code>null</code>)
     */
    public GitTFConfiguration(final URI serverURI, final String tfsPath)
    {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNullOrEmpty(tfsPath, "tfsPath"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.tfsPath = tfsPath;

        this.locallyDefinedNames = new HashMap<String, Boolean>();
        locallyDefinedNames.put(ConfigurationConstants.SERVER_COLLECTION_URI, true);
        locallyDefinedNames.put(ConfigurationConstants.SERVER_PATH, true);
    }

    /**
     * @return The URI of the TFS server (never <code>null</code>)
     */
    public URI getServerURI()
    {
        return serverURI;
    }

    /**
     * @return The server path bridged to the git repository (never
     *         <code>null</code>)
     */
    public String getServerPath()
    {
        return tfsPath;
    }

    /**
     * Returns the username to connect to TFS as. If none has been saved, the
     * client should attempt to connect with default credentials (Kerberos) if
     * available and provide a prompt to the user if those are not available or
     * do not succeed.
     * 
     * @return The username to connect to TFS as, or <code>null</code> if none
     *         has been saved
     */
    public String getUsername()
    {
        return username;
    }

    public static String getUsername(final Repository repository)
    {
        /*
         * The repository could be not initialized yet, in this case we'll get
         * the value (if any) from a global config file
         */
        return repository.getConfig().getString(
            ConfigurationConstants.CONFIGURATION_SECTION,
            ConfigurationConstants.SERVER_SUBSECTION,
            ConfigurationConstants.USERNAME);

    }

    /**
     * Returns the password to connect to TFS with. If none has been saved but
     * the username has been saved, the client should prompt for password.
     * 
     * @return The password to connect to TFS with, or <code>null</code> if none
     *         has been saved
     */
    public String getPassword()
    {
        return password;
    }

    public static String getPassword(final Repository repository)
    {
        /*
         * The repository could be not initialized yet, in this case we'll get
         * the value (if any) from a global config file
         */
        return repository.getConfig().getString(
            ConfigurationConstants.CONFIGURATION_SECTION,
            ConfigurationConstants.SERVER_SUBSECTION,
            ConfigurationConstants.PASSWORD);

    }

    /**
     * Returns the default "depth" for operations - if this value is
     * <code>1</code>, operations are "shallow" by default, meaning that
     * multiple git commits will be squashed to a single TFS changeset when
     * checking in, and multiple TFS changesets will be squashed to a single git
     * commit when fetching. If this value is {@link Integer#MAX_VALUE}, then
     * operations are "deep" and there will be a 1:1 correspondence between git
     * commits and TFS changesets whenever possible. Values between
     * <code>1</code> and {@link Integer#MAX_VALUE} will preserve some history.
     * 
     * @return the default depth
     */
    public boolean getDeep()
    {
        return deep;
    }

    public boolean getTag()
    {
        return tag;
    }

    /**
     * Returns the default setting for including metadata on changesets when
     * checking-in code to TFS in deep mode - if this value is <code>true</code>
     * , commit messages in TFS changesets will include git commit metadata. If
     * this value is <code>false</code>, then commit messages in TFS changesets
     * will have "clean" comments, i.e. the exact same messages defined in Git
     * commits.
     * 
     * @return the default setting for including metadata on changesets
     */
    public boolean getIncludeMetaData()
    {
        return includeMetaData;
    }

    public int getFileFormatVersion()
    {
        return fileFormatVersion;
    }

    public String getBuildDefinition()
    {
        return buildDefinition;
    }

    /**
     * Overrides the temp directory used for staging workspace changes. This may
     * be useful if you have git-tf fetched something inside an existing
     * workspace mapping.
     * 
     * @return The temp directory to use for staging changes, or
     *         <code>null</code> to use the default
     */
    public String getTempDirectory()
    {
        return tempDirectory;
    }

    public boolean getKeepAuthor()
    {
        return keepAuthor;
    }

    public String getUserMap()
    {
        return userMap;
    }

    /*
     * Configuration field setters. Each setter keeps track that the field has
     * changed along with changig the fields value
     */
    public void setUsername(final String username)
    {
        this.username = username;
        locallyDefinedNames.put(ConfigurationConstants.USERNAME, true);
    }

    public void setPassword(final String password)
    {
        this.password = password;
        locallyDefinedNames.put(ConfigurationConstants.PASSWORD, true);
    }

    public void setDeep(final boolean deep)
    {
        this.deep = deep;
        locallyDefinedNames.put(ConfigurationConstants.DEPTH, true);
    }

    public void setTag(final boolean tag)
    {
        this.tag = tag;
        locallyDefinedNames.put(ConfigurationConstants.TAG, true);
    }

    public void setIncludeMetaData(final boolean includeMetaData)
    {
        this.includeMetaData = includeMetaData;
        locallyDefinedNames.put(ConfigurationConstants.INCLUDE_METADATA, true);
    }

    public void setFileFormatVersion(final int fileFormatVersion)
    {
        this.fileFormatVersion = fileFormatVersion;
        locallyDefinedNames.put(ConfigurationConstants.FILE_FORMAT_VERSION, true);
    }

    public void setBuildDefinition(final String buildDefinition)
    {
        this.buildDefinition = buildDefinition;
        locallyDefinedNames.put(ConfigurationConstants.GATED_BUILD_DEFINITION, true);
    }

    public void setTempDirectory(final String tempDirectory)
    {
        this.tempDirectory = tempDirectory;
        locallyDefinedNames.put(ConfigurationConstants.TEMP_DIRECTORY, true);
    }

    public void setKeepAuthor(final boolean keepAuthor)
    {
        this.keepAuthor = keepAuthor;
        locallyDefinedNames.put(ConfigurationConstants.KEEP_AUTHOR, true);
    }

    public void setUserMap(final String userMap)
    {
        this.userMap = userMap;
        locallyDefinedNames.put(ConfigurationConstants.USER_MAP, true);
    }

    /**
     * Checks if the specified parameter has been explicitly defined in the
     * local config file or has to be saved in that config file.
     * 
     * @param name
     *        The name of the configuration parameter (must not be
     *        <code>null</code>)
     * 
     * @return <code>true</code> if the named parameter has been or has to be
     *         explicitly specified in the local config file
     */
    private boolean isLocallyDefined(final String name)
    {
        Check.notNull(name, "name"); //$NON-NLS-1$
        return locallyDefinedNames.containsKey(name);
    }

    /**
     * Saves this configuration to the given git repository's configuration.
     * 
     * @param repository
     *        The {@link Repository} to save this configuration to (must not be
     *        <code>null</code>)
     * @return <code>true</code> if the configuration was saved successfully
     */
    public boolean saveTo(final Repository repository)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        repository.getConfig().setString(
            ConfigurationConstants.CONFIGURATION_SECTION,
            ConfigurationConstants.SERVER_SUBSECTION,
            ConfigurationConstants.SERVER_COLLECTION_URI,
            serverURI.toASCIIString());

        repository.getConfig().setString(
            ConfigurationConstants.CONFIGURATION_SECTION,
            ConfigurationConstants.SERVER_SUBSECTION,
            ConfigurationConstants.SERVER_PATH,
            tfsPath);

        if (isLocallyDefined(ConfigurationConstants.DEPTH))
        {
            repository.getConfig().setInt(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.DEPTH,
                deep ? Integer.MAX_VALUE : 1);
        }

        if (isLocallyDefined(ConfigurationConstants.FILE_FORMAT_VERSION))
        {
            repository.getConfig().setInt(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.FILE_FORMAT_VERSION,
                GitTFConstants.GIT_TF_CURRENT_FORMAT_VERSION);
        }

        if (isLocallyDefined(ConfigurationConstants.TAG))
        {
            repository.getConfig().setBoolean(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.TAG,
                tag);
        }

        if (isLocallyDefined(ConfigurationConstants.INCLUDE_METADATA))
        {
            repository.getConfig().setBoolean(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.INCLUDE_METADATA,
                includeMetaData);
        }

        if (isLocallyDefined(ConfigurationConstants.KEEP_AUTHOR))
        {
            repository.getConfig().setBoolean(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.KEEP_AUTHOR,
                keepAuthor);
        }

        if (isLocallyDefined(ConfigurationConstants.USER_MAP))
        {
            if (!StringUtil.isNullOrEmpty(userMap))
            {
                repository.getConfig().setString(
                    ConfigurationConstants.CONFIGURATION_SECTION,
                    ConfigurationConstants.GENERAL_SUBSECTION,
                    ConfigurationConstants.USER_MAP,
                    userMap);
            }
            else
            {
                repository.getConfig().unset(
                    ConfigurationConstants.CONFIGURATION_SECTION,
                    ConfigurationConstants.GENERAL_SUBSECTION,
                    ConfigurationConstants.USER_MAP);
            }
        }

        if (isLocallyDefined(ConfigurationConstants.GATED_BUILD_DEFINITION)
            && !StringUtil.isNullOrEmpty(buildDefinition))
        {
            repository.getConfig().setString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.SERVER_SUBSECTION,
                ConfigurationConstants.GATED_BUILD_DEFINITION,
                buildDefinition);
        }

        if (isLocallyDefined(ConfigurationConstants.USERNAME))
        {
            repository.getConfig().setString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.SERVER_SUBSECTION,
                ConfigurationConstants.USERNAME,
                username);
        }

        if (isLocallyDefined(ConfigurationConstants.PASSWORD))
        {
            repository.getConfig().setString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.SERVER_SUBSECTION,
                ConfigurationConstants.PASSWORD,
                password);
        }

        if (isLocallyDefined(ConfigurationConstants.TEMP_DIRECTORY) && !StringUtil.isNullOrEmpty(tempDirectory))
        {
            repository.getConfig().setString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.TEMP_DIRECTORY,
                tempDirectory);
        }

        repository.getConfig().setEnum(
            ConfigConstants.CONFIG_CORE_SECTION,
            null,
            ConfigConstants.CONFIG_KEY_AUTOCRLF,
            AutoCRLF.FALSE);

        try
        {
            repository.getConfig().save();
        }
        catch (IOException e)
        {
            log.error("Could not save server configuration to repository", e); //$NON-NLS-1$
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        final StringBuilder result = new StringBuilder();

        result.append(Messages.formatString("GitTFConfiguration.ToString.ServerURIFormat", this.serverURI) + OutputConstants.NEW_LINE); //$NON-NLS-1$
        result.append(Messages.formatString("GitTFConfiguration.ToString.TfsPathFormat", this.tfsPath) + OutputConstants.NEW_LINE); //$NON-NLS-1$

        if (!StringUtil.isNullOrEmpty(buildDefinition))
        {
            result.append(Messages.formatString("GitTFConfiguration.ToString.GatedBuildFormat", this.buildDefinition) + OutputConstants.NEW_LINE); //$NON-NLS-1$
        }

        if (!StringUtil.isNullOrEmpty(tempDirectory))
        {
            result.append(Messages.formatString("GitTFConfiguration.ToString.TempDirectoryFormat", this.tempDirectory) + OutputConstants.NEW_LINE); //$NON-NLS-1$
        }

        result.append(Messages.formatString("GitTFConfiguration.ToString.DepthFormat", getDepthString()) + OutputConstants.NEW_LINE); //$NON-NLS-1$
        result.append(Messages.formatString("GitTFConfiguration.ToString.TagFormat", this.tag) + OutputConstants.NEW_LINE); //$NON-NLS-1$
        result.append(Messages.formatString("GitTFConfiguration.ToString.IncludeMetaDataFormat", this.includeMetaData) + OutputConstants.NEW_LINE); //$NON-NLS-1$
        result.append(Messages.formatString("GitTFConfiguration.KeepAuthorFormat", this.keepAuthor) + OutputConstants.NEW_LINE); //$NON-NLS-1$
        if (!StringUtil.isNullOrEmpty(userMap))
        {
            result.append(Messages.formatString("GitTFConfiguration.UserMapFormat", this.userMap) + OutputConstants.NEW_LINE); //$NON-NLS-1$

        }

        return result.toString();
    }

    private String getDepthString()
    {
        if (getDeep())
        {
            return Messages.getString("GitTFConfiguration.Deep"); //$NON-NLS-1$
        }
        else
        {
            return Messages.getString("GitTFConfiguration.Shallow"); //$NON-NLS-1$
        }
    }

    /**
     * Loads the git-tf configuration from the given git repository.
     * 
     * @param repository
     *        The {@link Repository} to load git-tf configuration data from
     *        (must not be <code>null</code>)
     * @return A new {@link GitTFConfiguration}, or <code>null</code> if the git
     *         repository does not contain a valid git-tf configuration
     */
    public static GitTFConfiguration loadFrom(final Repository repository)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        final String projectCollection =
            repository.getConfig().getString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.SERVER_SUBSECTION,
                ConfigurationConstants.SERVER_COLLECTION_URI);

        final String tfsPath =
            repository.getConfig().getString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.SERVER_SUBSECTION,
                ConfigurationConstants.SERVER_PATH);

        final String username =
            repository.getConfig().getString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.SERVER_SUBSECTION,
                ConfigurationConstants.USERNAME);

        final String password =
            repository.getConfig().getString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.SERVER_SUBSECTION,
                ConfigurationConstants.PASSWORD);

        final int depth =
            repository.getConfig().getInt(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.DEPTH,
                GitTFConstants.GIT_TF_SHALLOW_DEPTH);

        final boolean tag =
            repository.getConfig().getBoolean(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.TAG,
                true);

        final boolean includeMetaData =
            repository.getConfig().getBoolean(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.INCLUDE_METADATA,
                GitTFConstants.GIT_TF_DEFAULT_INCLUDE_METADATA);

        final int fileFormatVersion =
            repository.getConfig().getInt(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.FILE_FORMAT_VERSION,
                0);

        final String buildDefinition =
            repository.getConfig().getString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.SERVER_SUBSECTION,
                ConfigurationConstants.GATED_BUILD_DEFINITION);

        final String tempDirectory =
            repository.getConfig().getString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.TEMP_DIRECTORY);

        final boolean keepAuthor =
            repository.getConfig().getBoolean(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.KEEP_AUTHOR,
                GitTFConstants.GIT_TF_DEFAULT_KEEP_AUTHOR);

        final String userMap =
            repository.getConfig().getString(
                ConfigurationConstants.CONFIGURATION_SECTION,
                ConfigurationConstants.GENERAL_SUBSECTION,
                ConfigurationConstants.USER_MAP);

        if (projectCollection == null)
        {
            log.error("No project collection configuration in repository"); //$NON-NLS-1$
            return null;
        }

        if (tfsPath == null)
        {
            log.error("No TFS server path configuration in repository"); //$NON-NLS-1$
            return null;
        }

        URI serverURI;

        try
        {
            serverURI = new URI(projectCollection);
        }
        catch (URISyntaxException e)
        {
            log.error("TFS project collection URI is malformed", e); //$NON-NLS-1$
            return null;
        }

        final String[] sectionNames = new String[]
        {
            ConfigurationConstants.GENERAL_SUBSECTION, ConfigurationConstants.SERVER_SUBSECTION
        };
        final Map<String, Boolean> isDefined = new HashMap<String, Boolean>();

        for (final String sectionName : sectionNames)
        {
            final Set<String> definedNames =
                repository.getConfig().getNames(ConfigurationConstants.CONFIGURATION_SECTION, sectionName);

            for (final String name : definedNames)
            {
                isDefined.put(name, true);
            }
        }

        return new GitTFConfiguration(
            serverURI,
            tfsPath,
            username,
            password,
            depth > GitTFConstants.GIT_TF_SHALLOW_DEPTH,
            tag,
            includeMetaData,
            fileFormatVersion,
            buildDefinition,
            tempDirectory,
            keepAuthor,
            userMap,
            isDefined);
    }

    /**
     * Removes the git-tf configuration data from a git repository.
     * 
     * @param repository
     *        The {@link Repository} to remove configuration from (must not be
     *        <code>null</code>)
     */
    public static void removeFrom(final Repository repository)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        repository.getConfig().unsetSection(
            ConfigurationConstants.CONFIGURATION_SECTION,
            ConfigurationConstants.SERVER_SUBSECTION);

        repository.getConfig().unsetSection(
            ConfigurationConstants.CONFIGURATION_SECTION,
            ConfigurationConstants.GENERAL_SUBSECTION);
    }
}
