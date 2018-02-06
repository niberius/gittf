/*
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
 */

package com.microsoft.gittf.client.clc.commands;

import com.microsoft.gittf.client.clc.ExitCode;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.arguments.*;
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
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.net.URI;

/**
 * Clones a folder in TFS as a new git repository.
 */
public class CloneCommand
        extends Command {
    public static final String COMMAND_NAME = "clone";

    private static final Argument[] ARGUMENTS = new Argument[]
            {
                    new SwitchArgument("help",
                            Messages.getString("Command.Argument.Help.HelpText"),
                            ArgumentOptions.SUPPRESS_REQUIREMENTS),

                    new ChoiceArgument(Messages.getString("Command.Argument.Display.HelpText"),
                            new SwitchArgument("quiet",
                                    'q',
                                    Messages.getString("Command.Argument.Quiet.HelpText")),

                            new SwitchArgument("verbose",
                                    Messages.getString("Command.Argument.Verbose.HelpText"))
                    ),

                    new ValueArgument("version",
                            Messages.getString("Command.Argument.Version.ValueDescription"),
                            Messages.getString("CloneCommand.Argument.Version.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new SwitchArgument("bare",
                            Messages.getString("CloneCommand.Argument.Bare.HelpText")),

                    new ChoiceArgument(Messages.getString("CloneCommand.Argument.DepthChoice.HelpText"),
                            /* Users can specify one of --depth, --deep or --shallow. */
                            new SwitchArgument("deep",
                                    Messages.getString("CloneCommand.Argument.Deep.HelpText")),

                            new ValueArgument("depth",
                                    Messages.getString("CloneCommand.Argument.Depth.ValueDescription"),
                                    Messages.getString("CloneCommand.Argument.Depth.HelpText"),
                                    ArgumentOptions.VALUE_REQUIRED),

                            new SwitchArgument("shallow",
                                    Messages.getString("CloneCommand.Argument.Shallow.HelpText"))
                    ),

                    new ChoiceArgument(Messages.getString("Command.Argument.TagChoice.HelpText"),
                            /* Users can specify one of --tag or --no-tag (Default: tag). */
                            new SwitchArgument("tag",
                                    Messages.getString("Command.Argument.Tag.HelpText")),

                            new SwitchArgument("no-tag",
                                    Messages.getString("Command.Argument.NoTag.HelpText"))
                    ),

                    new SwitchArgument("mentions", Messages.getString("Command.Argument.Mentions.HelpText")),

                    new FreeArgument("projectcollection",
                            Messages.getString("Command.Argument.ProjectCollection.HelpText"),
                            ArgumentOptions.REQUIRED),

                    new FreeArgument("serverpath",
                            Messages.getString("Command.Argument.ServerPath.HelpText"),
                            ArgumentOptions.REQUIRED),

                    new FreeArgument("directory",
                            Messages.getString("CloneCommand.Argument.Directory.HelpText")),
            };

    @Override
    protected String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Argument[] getPossibleArguments() {
        return ARGUMENTS;
    }

    @Override
    public String getHelpDescription() {
        return Messages.getString("CloneCommand.HelpDescription");
    }

    @Override
    public int run()
            throws Exception {
        // Parse arguments
        final String collection = ((FreeArgument) getArguments().getArgument("projectcollection")).getValue();
        String tfsPath = ((FreeArgument) getArguments().getArgument("serverpath")).getValue();

        String repositoryPath = getArguments().contains("directory") ?
                ((FreeArgument) getArguments().getArgument("directory")).getValue() : null;

        final VersionSpec versionSpec =
                getArguments().contains("version") ?
                        VersionSpecUtil.parseVersionSpec(((ValueArgument) getArguments().getArgument("version")).getValue()) : LatestVersionSpec.INSTANCE;

        verifyVersionSpec(versionSpec);

        final boolean bare = getArguments().contains("bare");
        final int depth = getDepthFromArguments();

        final boolean mentions = getArguments().contains("mentions");
        if (mentions && depth < 2) {
            throw new Exception(Messages.getString("Command.MentionsOnlyAvailableWithDeep"));
        }

        final boolean tag = getTagFromArguments();

        final URI serverURI = URIUtil.getServerURI(collection);
        tfsPath = ServerPath.canonicalize(tfsPath);

        /*
         * Build repository path
         */
        if (repositoryPath == null) {
            repositoryPath = ServerPath.getFileName(tfsPath);
        }
        repositoryPath = LocalPath.canonicalize(repositoryPath);

        final File repositoryLocation = new File(repositoryPath);
        File parentLocationCreated = null;

        if (!repositoryLocation.exists()) {
            parentLocationCreated = DirectoryUtil.createDirectory(repositoryLocation);
            if (parentLocationCreated == null) {
                throw new Exception(Messages.formatString("CloneCommnad.InvalidPathFormat", repositoryPath));
            }
        }

        final Repository repository = RepositoryUtil.createNewRepository(repositoryPath, bare);

        /*
         * Connect to the server
         */
        try {
            final TFSTeamProjectCollection connection = getConnection(serverURI, repository);

            Check.notNull(connection, "connection");

            final WorkItemClient witClient = mentions ? connection.getWorkItemClient() : null;
            final CloneTask cloneTask =
                    new CloneTask(serverURI, getVersionControlService(), tfsPath, repository, witClient);

            cloneTask.setBare(bare);
            cloneTask.setDepth(depth);
            cloneTask.setVersionSpec(versionSpec);
            cloneTask.setTag(tag);

            final TaskStatus cloneStatus = new CommandTaskExecutor(getProgressMonitor()).execute(cloneTask);

            if (!cloneStatus.isOK()) {
                FileHelpers.deleteDirectory(bare ? repository.getDirectory() : repository.getWorkTree());

                if (parentLocationCreated != null) {
                    FileHelpers.deleteDirectory(parentLocationCreated);
                }

                return ExitCode.FAILURE;
            }
        } finally {
            repository.close();
        }

        return ExitCode.SUCCESS;
    }
}
