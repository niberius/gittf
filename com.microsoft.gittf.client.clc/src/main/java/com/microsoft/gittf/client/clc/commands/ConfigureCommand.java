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
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.tasks.ConfigureRepositoryTask;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.StringUtil;
import com.microsoft.gittf.core.util.URIUtil;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.net.URI;
import java.text.MessageFormat;

/**
 * Configures a git repository to be mapped to tfs.
 */
public class ConfigureCommand extends Command {
    public static final String COMMAND_NAME = "configure";

    private static Argument[] ARGUMENTS = new Argument[]
            {
                    new SwitchArgument("help", Messages.getString("Command.Argument.Help.HelpText")),

                    new ChoiceArgument(Messages.getString("Command.Argument.Display.HelpText"),
                            new SwitchArgument("quiet",
                                    'q',
                                    Messages.getString("Command.Argument.Quiet.HelpText")),

                            new SwitchArgument("verbose",
                                    Messages.getString("Command.Argument.Verbose.HelpText"))
                    ),

                    new SwitchArgument("list", 'l', Messages.getString("ConfigureCommand.Argument.List.HelpText")),

                    new SwitchArgument("force",
                            'f',
                            Messages.getString("ConfigureCommand.Argument.Force.HelpText")),

                    new ChoiceArgument(Messages.getString("Command.Argument.DepthChoice.HelpText"),

                            /* Users can specify one of --deep, --depth or --shallow. */
                            new SwitchArgument("deep",
                                    Messages.getString("Command.Argument.Deep.HelpText")),

                            new SwitchArgument("shallow",
                                    Messages.getString("Command.Argument.Shallow.HelpText"))
                    ),

                    new ValueArgument("gated",
                            'g',
                            Messages.getString("ConfigureCommand.Argument.Gated.ValueDescription"),
                            Messages.getString("ConfigureCommand.Argument.Gated.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new ChoiceArgument(Messages.getString("Command.Argument.TagChoice.HelpText"),
                            /* Users can specify one of --tag or --no-tag (Default: tag). */
                            new SwitchArgument("tag",
                                    Messages.getString("Command.Argument.Tag.HelpText")),

                            new SwitchArgument("no-tag",
                                    Messages.getString("Command.Argument.NoTag.HelpText"))
                    ),

                    new ChoiceArgument(Messages.getString("Command.Argument.MetaDataChoice.HelpText"),
                            /*
                             * Users can specify one of --metadata or --no-metadata (Default:
                             * no-metadata).
                             */
                            new SwitchArgument("metadata",
                                    Messages.getString("Command.Argument.MetaData.HelpText")),

                            new SwitchArgument("no-metadata",
                                    Messages.getString("Command.Argument.NoMetaData.HelpText"))
                    ),

                    new ValueArgument("git-dir",
                            Messages.getString("CloneCommand.Argument.GitDir.ValueDescription"),
                            Messages.getString("CloneCommand.Argument.GitDir.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new ChoiceArgument(Messages.getString("ConfigureCommand.Argument.KeepAuthorChoice.HelpText"),
                            /*
                             * Users can specify one of --keep-author or --ignore-author
                             * (Default: ignore-author).
                             */
                            new SwitchArgument("keep-author", Messages.getString("CheckinCommand.Argument.KeepAuthor.HelpText")),
                            new SwitchArgument("ignore-author", Messages.getString("CheckinCommand.Argument.IgnoreAuthor.HelpText"))
                    ),

                    new ValueArgument("user-map",
                            Messages.getString("CheckinCommand.Argument.UserMap.ValueDescription"),
                            Messages.getString("CheckinCommand.Argument.UserMap.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new ValueArgument("username",
                            Messages.getString("CloneCommand.Argument.UserName.ValueDescription"),
                            Messages.getString("CloneCommand.Argument.UserName.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new ValueArgument("password",
                            Messages.getString("CloneCommand.Argument.Password.ValueDescription"),
                            Messages.getString("CloneCommand.Argument.Password.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new FreeArgument("projectcollection",
                            Messages.getString("Command.Argument.ProjectCollection.HelpText")),

                    new FreeArgument("serverpath",
                            Messages.getString("Command.Argument.ServerPath.HelpText")),
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
        return Messages.getString("ConfigureCommand.HelpDescription");
    }

    @Override
    public int run()
            throws Exception {
        // Determine if there is current configuration that we need to update
        final Repository repository = getRepository();
        final GitTFConfiguration currentConfiguration = GitTFConfiguration.loadFrom(repository);

        /*
         * If the list option is specified we just display the configuration
         * options
         */
        if (getArguments().contains("list") || getArguments().getArguments().size() <= 0)
        {
            if (currentConfiguration == null) {
                // Not configured
                throw new Exception(Messages.getString("ConfigureCommand.GitRepoNotConfigured"));
            } else {
                // Display configuration
                getConsole().getOutputStream().println(currentConfiguration.toString());
            }

            return ExitCode.SUCCESS;
        }

        URI serverURI;
        String tfsPath;

        if (currentConfiguration == null || getArguments().contains("force"))
        {
            // Parse arguments
            String collection = getArguments().contains("projectcollection") ?
                    ((FreeArgument) getArguments().getArgument("projectcollection")).getValue() : null;

            tfsPath = getArguments().contains("serverpath") ?
                    ((FreeArgument) getArguments().getArgument("serverpath")).getValue() : null;

            // Validate arguments
            if (StringUtil.isNullOrEmpty(collection) || StringUtil.isNullOrEmpty(tfsPath)) {
                throw new Exception(Messages.getString("ConfigureCommand.CollectionAndServerPathRequired"));
            }

            serverURI = URIUtil.getServerURI(collection);

            if (serverURI == null) {
                throw new Exception(Messages.formatString("ConfigureCommand.InvalidCollectionFormat",
                        collection));
            }

            tfsPath = ServerPath.canonicalize(tfsPath);
        } else {
            serverURI = currentConfiguration.getServerURI();
            tfsPath = currentConfiguration.getServerPath();

            if (!getArguments().contains("deep") &&
                    !getArguments().contains("shallow") &&
                    !getArguments().contains("tag") &&
                    !getArguments().contains("no-tag") &&
                    !getArguments().contains("metadata") &&
                    !getArguments().contains("no-metadata") &&
                    !getArguments().contains("gated") &&
                    !getArguments().contains("keep-author") &&
                    !getArguments().contains("ignore-author") &&
                    !getArguments().contains("username") &&
                    !getArguments().contains("password") &&
                    !getArguments().contains("user-map"))
            {
                throw new Exception(Messages.getString("ConfigureCommand.InvalidOptionsSpecified"));
            }
        }

        final ConfigureRepositoryTask configureTask = new ConfigureRepositoryTask(repository, serverURI, tfsPath);

        if (getArguments().contains("deep"))
        {
            configureTask.setDeep(true);
        } else if (getArguments().contains("shallow"))
        {
            configureTask.setDeep(false);
        }

        if (getArguments().contains("tag"))
        {
            configureTask.setTag(true);
        } else if (getArguments().contains("no-tag"))
        {
            configureTask.setTag(false);
        }

        if (getArguments().contains("metadata"))
        {
            configureTask.setIncludeMetaData(true);
        } else if (getArguments().contains("no-metadata"))
        {
            configureTask.setIncludeMetaData(false);
        }

        if (getArguments().contains("gated"))
        {
            final String buildDefinition = ((ValueArgument) getArguments().getArgument("gated")).getValue();
            configureTask.setBuildDefinition(buildDefinition);
        }

        if (getArguments().contains("keep-author"))
        {
            configureTask.setKeepAuthor(true);
        } else if (getArguments().contains("ignore-author"))
        {
            configureTask.setKeepAuthor(false);
        }

        if (getArguments().contains("user-map"))
        {
            final String userMap = ((ValueArgument) getArguments().getArgument("user-map")).getValue();
            if (isValidPath(userMap)) {
                configureTask.setUserMap(userMap);
            }
        }

        if (getArguments().contains("username"))
        {
            final String username = ((ValueArgument) getArguments().getArgument("username")).getValue();
            configureTask.setUsername(username);
        }

        if (getArguments().contains("password"))
        {
            final String password = ((ValueArgument) getArguments().getArgument("password")).getValue();
            configureTask.setPassword(password);
        }

        configureTask.setTempDirectory(null);

        TaskStatus configureStatus = new CommandTaskExecutor(getProgressMonitor()).execute(configureTask);

        if (!configureStatus.isOK()) {
            return ExitCode.FAILURE;
        }

        return ExitCode.SUCCESS;
    }

    private boolean isValidPath(final String path)
            throws Exception {
        if (StringUtil.isNullOrEmpty(path)) {
            return true;
        }

        try {
            (new File(path)).getCanonicalFile();
            return true;
        } catch (final Exception e) {
            final String errorMessageFormat = Messages.getString("ConfigureCommand.IncorrectPathFormat");
            throw new Exception(MessageFormat.format(errorMessageFormat, path));
        }
    }

    @Override
    protected boolean isMultiRepositories() {
        return false;
    }
}
