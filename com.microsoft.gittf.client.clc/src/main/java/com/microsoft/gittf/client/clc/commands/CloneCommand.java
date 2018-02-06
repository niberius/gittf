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

package com.microsoft.gittf.client.clc.commands;

import java.io.File;
import java.net.URI;

import org.eclipse.jgit.lib.Repository;

import com.microsoft.gittf.client.clc.ExitCode;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.arguments.Argument;
import com.microsoft.gittf.client.clc.arguments.ArgumentOptions;
import com.microsoft.gittf.client.clc.arguments.ChoiceArgument;
import com.microsoft.gittf.client.clc.arguments.FreeArgument;
import com.microsoft.gittf.client.clc.arguments.SwitchArgument;
import com.microsoft.gittf.client.clc.arguments.ValueArgument;
import com.microsoft.gittf.client.clc.commands.framework.Command;
import com.microsoft.gittf.client.clc.commands.framework.CommandTaskExecutor;
import com.microsoft.gittf.core.tasks.CloneTask;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.DirectoryUtil;
import com.microsoft.gittf.core.util.RepositoryUtil;
import com.microsoft.gittf.core.util.URIUtil;
import com.microsoft.gittf.core.util.VersionSpecUtil;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;

/**
 * Clones a folder in TFS as a new git repository.
 * 
 */
public class CloneCommand
    extends Command
{
    public static final String COMMAND_NAME = "clone"; //$NON-NLS-1$

    private static final Argument[] ARGUMENTS = new Argument[]
    {
        new SwitchArgument("help", //$NON-NLS-1$
            Messages.getString("Command.Argument.Help.HelpText"), //$NON-NLS-1$
            ArgumentOptions.SUPPRESS_REQUIREMENTS),

        new ChoiceArgument(Messages.getString("Command.Argument.Display.HelpText"), //$NON-NLS-1$
            new SwitchArgument("quiet", //$NON-NLS-1$
                'q',
                Messages.getString("Command.Argument.Quiet.HelpText")), //$NON-NLS-1$

            new SwitchArgument("verbose", //$NON-NLS-1$
                Messages.getString("Command.Argument.Verbose.HelpText")) //$NON-NLS-1$
        ),

        new ValueArgument("version", //$NON-NLS-1$
            Messages.getString("Command.Argument.Version.ValueDescription"), //$NON-NLS-1$
            Messages.getString("CloneCommand.Argument.Version.HelpText"), //$NON-NLS-1$
            ArgumentOptions.VALUE_REQUIRED),

        new SwitchArgument("bare", //$NON-NLS-1$
            Messages.getString("CloneCommand.Argument.Bare.HelpText")), //$NON-NLS-1$

        new ChoiceArgument(Messages.getString("CloneCommand.Argument.DepthChoice.HelpText"), //$NON-NLS-1$
            /* Users can specify one of --depth, --deep or --shallow. */
            new SwitchArgument("deep", //$NON-NLS-1$
                Messages.getString("CloneCommand.Argument.Deep.HelpText")), //$NON-NLS-1$

            new ValueArgument("depth", //$NON-NLS-1$
                Messages.getString("CloneCommand.Argument.Depth.ValueDescription"), //$NON-NLS-1$
                Messages.getString("CloneCommand.Argument.Depth.HelpText"), //$NON-NLS-1$
                ArgumentOptions.VALUE_REQUIRED),

            new SwitchArgument("shallow", //$NON-NLS-1$
                Messages.getString("CloneCommand.Argument.Shallow.HelpText")) //$NON-NLS-1$
        ),

        new ChoiceArgument(Messages.getString("Command.Argument.TagChoice.HelpText"), //$NON-NLS-1$
            /* Users can specify one of --tag or --no-tag (Default: tag). */
            new SwitchArgument("tag", //$NON-NLS-1$
                Messages.getString("Command.Argument.Tag.HelpText")), //$NON-NLS-1$

            new SwitchArgument("no-tag", //$NON-NLS-1$
                Messages.getString("Command.Argument.NoTag.HelpText")) //$NON-NLS-1$
        ),

        new SwitchArgument("mentions", Messages.getString("Command.Argument.Mentions.HelpText")), //$NON-NLS-1$ //$NON-NLS-2$

        new FreeArgument("projectcollection", //$NON-NLS-1$
            Messages.getString("Command.Argument.ProjectCollection.HelpText"), //$NON-NLS-1$
            ArgumentOptions.REQUIRED),

        new FreeArgument("serverpath", //$NON-NLS-1$
            Messages.getString("Command.Argument.ServerPath.HelpText"), //$NON-NLS-1$
            ArgumentOptions.REQUIRED),

        new FreeArgument("directory", //$NON-NLS-1$
            Messages.getString("CloneCommand.Argument.Directory.HelpText")), //$NON-NLS-1$
    };

    @Override
    protected String getCommandName()
    {
        return COMMAND_NAME;
    }

    @Override
    public Argument[] getPossibleArguments()
    {
        return ARGUMENTS;
    }

    @Override
    public String getHelpDescription()
    {
        return Messages.getString("CloneCommand.HelpDescription"); //$NON-NLS-1$
    }

    @Override
    public int run()
        throws Exception
    {
        // Parse arguments
        final String collection = ((FreeArgument) getArguments().getArgument("projectcollection")).getValue(); //$NON-NLS-1$
        String tfsPath = ((FreeArgument) getArguments().getArgument("serverpath")).getValue(); //$NON-NLS-1$

        String repositoryPath = getArguments().contains("directory") ? //$NON-NLS-1$
            ((FreeArgument) getArguments().getArgument("directory")).getValue() : null; //$NON-NLS-1$

        final VersionSpec versionSpec =
            getArguments().contains("version") ? //$NON-NLS-1$
                VersionSpecUtil.parseVersionSpec(((ValueArgument) getArguments().getArgument("version")).getValue()) : LatestVersionSpec.INSTANCE; //$NON-NLS-1$

        verifyVersionSpec(versionSpec);

        final boolean bare = getArguments().contains("bare"); //$NON-NLS-1$
        final int depth = getDepthFromArguments();

        final boolean mentions = getArguments().contains("mentions"); //$NON-NLS-1$
        if (mentions && depth < 2)
        {
            throw new Exception(Messages.getString("Command.MentionsOnlyAvailableWithDeep")); //$NON-NLS-1$
        }

        final boolean tag = getTagFromArguments();

        final URI serverURI = URIUtil.getServerURI(collection);
        tfsPath = ServerPath.canonicalize(tfsPath);

        /*
         * Build repository path
         */
        if (repositoryPath == null)
        {
            repositoryPath = ServerPath.getFileName(tfsPath);
        }
        repositoryPath = LocalPath.canonicalize(repositoryPath);

        final File repositoryLocation = new File(repositoryPath);
        File parentLocationCreated = null;

        if (!repositoryLocation.exists())
        {
            parentLocationCreated = DirectoryUtil.createDirectory(repositoryLocation);
            if (parentLocationCreated == null)
            {
                throw new Exception(Messages.formatString("CloneCommnad.InvalidPathFormat", repositoryPath)); //$NON-NLS-1$
            }
        }

        final Repository repository = RepositoryUtil.createNewRepository(repositoryPath, bare);

        /*
         * Connect to the server
         */
        try
        {
            final TFSTeamProjectCollection connection = getConnection(serverURI, repository);

            Check.notNull(connection, "connection"); //$NON-NLS-1$

            final WorkItemClient witClient = mentions ? connection.getWorkItemClient() : null;
            final CloneTask cloneTask =
                new CloneTask(serverURI, getVersionControlService(), tfsPath, repository, witClient);

            cloneTask.setBare(bare);
            cloneTask.setDepth(depth);
            cloneTask.setVersionSpec(versionSpec);
            cloneTask.setTag(tag);

            final TaskStatus cloneStatus = new CommandTaskExecutor(getProgressMonitor()).execute(cloneTask);

            if (!cloneStatus.isOK())
            {
                FileHelpers.deleteDirectory(bare ? repository.getDirectory() : repository.getWorkTree());

                if (parentLocationCreated != null)
                {
                    FileHelpers.deleteDirectory(parentLocationCreated);
                }

                return ExitCode.FAILURE;
            }
        }
        finally
        {
            repository.close();
        }

        return ExitCode.SUCCESS;
    }
}
