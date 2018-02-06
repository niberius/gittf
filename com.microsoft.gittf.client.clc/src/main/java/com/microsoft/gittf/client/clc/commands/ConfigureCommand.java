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
import java.text.MessageFormat;

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
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.tasks.ConfigureRepositoryTask;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.StringUtil;
import com.microsoft.gittf.core.util.URIUtil;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

/**
 * Configures a git repository to be mapped to tfs.
 * 
 */
public class ConfigureCommand
    extends Command
{
    public static final String COMMAND_NAME = "configure"; //$NON-NLS-1$

    private static Argument[] ARGUMENTS = new Argument[]
    {
        new SwitchArgument("help", Messages.getString("Command.Argument.Help.HelpText")), //$NON-NLS-1$ //$NON-NLS-2$

        new ChoiceArgument(Messages.getString("Command.Argument.Display.HelpText"), //$NON-NLS-1$
            new SwitchArgument("quiet", //$NON-NLS-1$
                'q',
                Messages.getString("Command.Argument.Quiet.HelpText")), //$NON-NLS-1$

            new SwitchArgument("verbose", //$NON-NLS-1$
                Messages.getString("Command.Argument.Verbose.HelpText")) //$NON-NLS-1$
        ),

        new SwitchArgument("list", 'l', Messages.getString("ConfigureCommand.Argument.List.HelpText")), //$NON-NLS-1$ //$NON-NLS-2$

        new SwitchArgument("force", //$NON-NLS-1$
            'f',
            Messages.getString("ConfigureCommand.Argument.Force.HelpText")), //$NON-NLS-1$

        new ChoiceArgument(Messages.getString("Command.Argument.DepthChoice.HelpText"), //$NON-NLS-1$

            /* Users can specify one of --deep, --depth or --shallow. */
            new SwitchArgument("deep", //$NON-NLS-1$
                Messages.getString("Command.Argument.Deep.HelpText")), //$NON-NLS-1$

            new SwitchArgument("shallow", //$NON-NLS-1$
                Messages.getString("Command.Argument.Shallow.HelpText")) //$NON-NLS-1$
        ),

        new ValueArgument("gated", //$NON-NLS-1$
            'g',
            Messages.getString("ConfigureCommand.Argument.Gated.ValueDescription"), //$NON-NLS-1$
            Messages.getString("ConfigureCommand.Argument.Gated.HelpText"), //$NON-NLS-1$
            ArgumentOptions.VALUE_REQUIRED),

        new ChoiceArgument(Messages.getString("Command.Argument.TagChoice.HelpText"), //$NON-NLS-1$
            /* Users can specify one of --tag or --no-tag (Default: tag). */
            new SwitchArgument("tag", //$NON-NLS-1$
                Messages.getString("Command.Argument.Tag.HelpText")), //$NON-NLS-1$

            new SwitchArgument("no-tag", //$NON-NLS-1$
                Messages.getString("Command.Argument.NoTag.HelpText")) //$NON-NLS-1$
        ),

        new ChoiceArgument(Messages.getString("Command.Argument.MetaDataChoice.HelpText"), //$NON-NLS-1$
            /*
             * Users can specify one of --metadata or --no-metadata (Default:
             * no-metadata).
             */
            new SwitchArgument("metadata", //$NON-NLS-1$
                Messages.getString("Command.Argument.MetaData.HelpText")), //$NON-NLS-1$

            new SwitchArgument("no-metadata", //$NON-NLS-1$
                Messages.getString("Command.Argument.NoMetaData.HelpText")) //$NON-NLS-1$
        ),

        new ValueArgument("git-dir", //$NON-NLS-1$
            Messages.getString("CloneCommand.Argument.GitDir.ValueDescription"), //$NON-NLS-1$
            Messages.getString("CloneCommand.Argument.GitDir.HelpText"), //$NON-NLS-1$)
            ArgumentOptions.VALUE_REQUIRED),

        new ChoiceArgument(Messages.getString("ConfigureCommand.Argument.KeepAuthorChoice.HelpText"), //$NON-NLS-1$
            /*
             * Users can specify one of --keep-author or --ignore-author
             * (Default: ignore-author).
             */
            new SwitchArgument("keep-author", Messages.getString("CheckinCommand.Argument.KeepAuthor.HelpText")), //$NON-NLS-1$ //$NON-NLS-2$
            new SwitchArgument("ignore-author", Messages.getString("CheckinCommand.Argument.IgnoreAuthor.HelpText")) //$NON-NLS-1$ //$NON-NLS-2$
        ),

        new ValueArgument("user-map", //$NON-NLS-1$
            Messages.getString("CheckinCommand.Argument.UserMap.ValueDescription"), //$NON-NLS-1$
            Messages.getString("CheckinCommand.Argument.UserMap.HelpText"), //$NON-NLS-1$)
            ArgumentOptions.VALUE_REQUIRED),

        new ValueArgument("username", //$NON-NLS-1$
            Messages.getString("CloneCommand.Argument.UserName.ValueDescription"), //$NON-NLS-1$
            Messages.getString("CloneCommand.Argument.UserName.HelpText"), //$NON-NLS-1$)
            ArgumentOptions.VALUE_REQUIRED),

        new ValueArgument("password", //$NON-NLS-1$
            Messages.getString("CloneCommand.Argument.Password.ValueDescription"), //$NON-NLS-1$
            Messages.getString("CloneCommand.Argument.Password.HelpText"), //$NON-NLS-1$)
            ArgumentOptions.VALUE_REQUIRED),

        new FreeArgument("projectcollection", //$NON-NLS-1$
            Messages.getString("Command.Argument.ProjectCollection.HelpText")), //$NON-NLS-1$

        new FreeArgument("serverpath", //$NON-NLS-1$
            Messages.getString("Command.Argument.ServerPath.HelpText")), //$NON-NLS-1$
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
        return Messages.getString("ConfigureCommand.HelpDescription"); //$NON-NLS-1$
    }

    @Override
    public int run()
        throws Exception
    {
        // Determine if there is current configuration that we need to update
        final Repository repository = getRepository();
        final GitTFConfiguration currentConfiguration = GitTFConfiguration.loadFrom(repository);

        /*
         * If the list option is specified we just display the configuration
         * options
         */
        if (getArguments().contains("list") || getArguments().getArguments().size() <= 0) //$NON-NLS-1$
        {
            if (currentConfiguration == null)
            {
                // Not configured
                throw new Exception(Messages.getString("ConfigureCommand.GitRepoNotConfigured")); //$NON-NLS-1$
            }
            else
            {
                // Display configuration
                getConsole().getOutputStream().println(currentConfiguration.toString());
            }

            return ExitCode.SUCCESS;
        }

        URI serverURI;
        String tfsPath;

        if (currentConfiguration == null || getArguments().contains("force")) //$NON-NLS-1$
        {
            // Parse arguments
            String collection = getArguments().contains("projectcollection") ? //$NON-NLS-1$
                ((FreeArgument) getArguments().getArgument("projectcollection")).getValue() : null; //$NON-NLS-1$

            tfsPath = getArguments().contains("serverpath") ? //$NON-NLS-1$
                ((FreeArgument) getArguments().getArgument("serverpath")).getValue() : null; //$NON-NLS-1$

            // Validate arguments
            if (StringUtil.isNullOrEmpty(collection) || StringUtil.isNullOrEmpty(tfsPath))
            {
                throw new Exception(Messages.getString("ConfigureCommand.CollectionAndServerPathRequired")); //$NON-NLS-1$
            }

            serverURI = URIUtil.getServerURI(collection);

            if (serverURI == null)
            {
                throw new Exception(Messages.formatString("ConfigureCommand.InvalidCollectionFormat", //$NON-NLS-1$
                    collection));
            }

            tfsPath = ServerPath.canonicalize(tfsPath);
        }
        else
        {
            serverURI = currentConfiguration.getServerURI();
            tfsPath = currentConfiguration.getServerPath();

            if (!getArguments().contains("deep") && //$NON-NLS-1$
                !getArguments().contains("shallow") && //$NON-NLS-1$
                !getArguments().contains("tag") && //$NON-NLS-1$
                !getArguments().contains("no-tag") && //$NON-NLS-1$
                !getArguments().contains("metadata") && //$NON-NLS-1$
                !getArguments().contains("no-metadata") && //$NON-NLS-1$
                !getArguments().contains("gated") && //$NON-NLS-1$
                !getArguments().contains("keep-author") && //$NON-NLS-1$
                !getArguments().contains("ignore-author") && //$NON-NLS-1$
                !getArguments().contains("username") && //$NON-NLS-1$
                !getArguments().contains("password") && //$NON-NLS-1$
                !getArguments().contains("user-map")) //$NON-NLS-1$ 
            {
                throw new Exception(Messages.getString("ConfigureCommand.InvalidOptionsSpecified")); //$NON-NLS-1$
            }
        }

        final ConfigureRepositoryTask configureTask = new ConfigureRepositoryTask(repository, serverURI, tfsPath);

        if (getArguments().contains("deep")) //$NON-NLS-1$
        {
            configureTask.setDeep(true);
        }
        else if (getArguments().contains("shallow")) //$NON-NLS-1$
        {
            configureTask.setDeep(false);
        }

        if (getArguments().contains("tag")) //$NON-NLS-1$
        {
            configureTask.setTag(true);
        }
        else if (getArguments().contains("no-tag")) //$NON-NLS-1$
        {
            configureTask.setTag(false);
        }

        if (getArguments().contains("metadata")) //$NON-NLS-1$
        {
            configureTask.setIncludeMetaData(true);
        }
        else if (getArguments().contains("no-metadata")) //$NON-NLS-1$
        {
            configureTask.setIncludeMetaData(false);
        }

        if (getArguments().contains("gated")) //$NON-NLS-1$
        {
            final String buildDefinition = ((ValueArgument) getArguments().getArgument("gated")).getValue(); //$NON-NLS-1$
            configureTask.setBuildDefinition(buildDefinition);
        }

        if (getArguments().contains("keep-author")) //$NON-NLS-1$
        {
            configureTask.setKeepAuthor(true);
        }
        else if (getArguments().contains("ignore-author")) //$NON-NLS-1$
        {
            configureTask.setKeepAuthor(false);
        }

        if (getArguments().contains("user-map")) //$NON-NLS-1$
        {
            final String userMap = ((ValueArgument) getArguments().getArgument("user-map")).getValue(); //$NON-NLS-1$
            if (isValidPath(userMap))
            {
                configureTask.setUserMap(userMap);
            }
        }

        if (getArguments().contains("username")) //$NON-NLS-1$
        {
            final String username = ((ValueArgument) getArguments().getArgument("username")).getValue(); //$NON-NLS-1$
            configureTask.setUsername(username);
        }

        if (getArguments().contains("password")) //$NON-NLS-1$
        {
            final String password = ((ValueArgument) getArguments().getArgument("password")).getValue(); //$NON-NLS-1$
            configureTask.setPassword(password);
        }

        configureTask.setTempDirectory(null);

        TaskStatus configureStatus = new CommandTaskExecutor(getProgressMonitor()).execute(configureTask);

        if (!configureStatus.isOK())
        {
            return ExitCode.FAILURE;
        }

        return ExitCode.SUCCESS;
    }

    private boolean isValidPath(final String path)
        throws Exception
    {
        if (StringUtil.isNullOrEmpty(path))
        {
            return true;
        }

        try
        {
            (new File(path)).getCanonicalFile();
            return true;
        }
        catch (final Exception e)
        {
            final String errorMessageFormat = Messages.getString("ConfigureCommand.IncorrectPathFormat"); //$NON-NLS-1$
            throw new Exception(MessageFormat.format(errorMessageFormat, path));
        }
    }
}
