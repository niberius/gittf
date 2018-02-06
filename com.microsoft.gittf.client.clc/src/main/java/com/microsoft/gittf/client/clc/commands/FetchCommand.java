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

import com.microsoft.gittf.client.clc.ExitCode;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.arguments.Argument;
import com.microsoft.gittf.client.clc.arguments.ArgumentOptions;
import com.microsoft.gittf.client.clc.arguments.ChoiceArgument;
import com.microsoft.gittf.client.clc.arguments.SwitchArgument;
import com.microsoft.gittf.client.clc.arguments.ValueArgument;
import com.microsoft.gittf.client.clc.commands.framework.Command;
import com.microsoft.gittf.client.clc.commands.framework.CommandTaskExecutor;
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.tasks.FetchTask;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.VersionSpecUtil;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;

public class FetchCommand
    extends Command
{
    public static final String COMMAND_NAME = "fetch"; //$NON-NLS-1$

    private static final Argument[] ARGUMENTS = new Argument[]
    {
        new SwitchArgument("help", Messages.getString("Command.Argument.Help.HelpText")), //$NON-NLS-1$ //$NON-NLS-2$

        new ChoiceArgument(Messages.getString("Command.Argument.Display.HelpText"), //$NON-NLS-1$
            new SwitchArgument("quiet", //$NON-NLS-1$
                'q',
                Messages.getString("Command.Argument.Quiet.HelpText")), //$NON-NLS-1$

            new SwitchArgument("verbose", //$NON-NLS-1$
                Messages.getString("Command.Argument.Verbose.HelpText")) //$NON-NLS-1$
        ),

        new ValueArgument("version", //$NON-NLS-1$
            Messages.getString("Command.Argument.Version.ValueDescription"), //$NON-NLS-1$
            Messages.getString("FetchCommand.Argument.Version.HelpText"), //$NON-NLS-1$
            ArgumentOptions.VALUE_REQUIRED),

        new ChoiceArgument(Messages.getString("FetchCommand.Argument.DepthChoice.HelpText"), //$NON-NLS-1$

            /* Users can specify one of --depth, --deep or --shallow. */
            new SwitchArgument("deep", //$NON-NLS-1$
                Messages.getString("FetchCommand.Argument.Deep.HelpText")), //$NON-NLS-1$

            new SwitchArgument("shallow", //$NON-NLS-1$
                Messages.getString("FetchCommand.Argument.Shallow.HelpText")) //$NON-NLS-1$
        ),

        new SwitchArgument("force", Messages.getString("FetchCommand.Argument.Force.HelpText")), //$NON-NLS-1$ //$NON-NLS-2$

        new SwitchArgument("mentions", Messages.getString("Command.Argument.Mentions.HelpText")), //$NON-NLS-1$ //$NON-NLS-2$
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
        return Messages.getString("FetchCommand.HelpDescription"); //$NON-NLS-1$
    }

    @Override
    public int run()
        throws Exception
    {
        verifyGitTfConfigured();

        final VersionSpec versionSpec =
            getArguments().contains("version") ? //$NON-NLS-1$
                VersionSpecUtil.parseVersionSpec(((ValueArgument) getArguments().getArgument("version")).getValue()) : LatestVersionSpec.INSTANCE; //$NON-NLS-1$

        verifyVersionSpec(versionSpec);

        boolean deep = GitTFConfiguration.loadFrom(getRepository()).getDeep();
        if (isDepthSpecified())
        {
            deep = getDeepFromArguments();
        }

        final boolean mentions = getArguments().contains("mentions"); //$NON-NLS-1$
        if (mentions && !deep)
        {
            throw new Exception(Messages.getString("Command.MentionsOnlyAvailableWithDeep")); //$NON-NLS-1$
        }

        final boolean force = getArguments().contains("force"); //$NON-NLS-1$

        final WorkItemClient witClient = mentions ? getConnection().getWorkItemClient() : null;
        final FetchTask fetchTask = new FetchTask(getRepository(), getVersionControlService(), witClient);
        fetchTask.setVersionSpec(versionSpec);
        fetchTask.setDeep(deep);
        fetchTask.setForce(force);

        final TaskStatus fetchStatus = new CommandTaskExecutor(getProgressMonitor()).execute(fetchTask);

        return fetchStatus.isOK() ? ExitCode.SUCCESS : ExitCode.FAILURE;
    }
}
