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
import com.microsoft.gittf.client.clc.arguments.FreeArgument;
import com.microsoft.gittf.client.clc.arguments.SwitchArgument;
import com.microsoft.gittf.client.clc.arguments.ValueArgument;
import com.microsoft.gittf.client.clc.commands.framework.Command;
import com.microsoft.gittf.client.clc.commands.framework.CommandTaskExecutor;
import com.microsoft.gittf.core.tasks.UnshelveTask;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;

public class UnshelveCommand
    extends Command
{
    public static final String COMMAND_NAME = "unshelve"; //$NON-NLS-1$

    private static Argument[] ARGUMENTS =
        new Argument[]
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

            //new SwitchArgument("apply", 'a', Messages.getString("UnshelveCommand.Argument.Apply.HelpText")), //$NON-NLS-1$ //$NON-NLS-2$

            new ValueArgument("user", //$NON-NLS-1$
                'u',
                Messages.getString("UnshelveCommand.Argument.User.ValueDescription"), //$NON-NLS-1$
                Messages.getString("UnshelveCommand.Argument.User.HelpText"), //$NON-NLS-1$
                ArgumentOptions.VALUE_REQUIRED),

            new FreeArgument(
                "name", Messages.getString("UnshelveCommand.Argument.Name.HelpText"), ArgumentOptions.REQUIRED) //$NON-NLS-1$ //$NON-NLS-2$
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
        return Messages.getString("UnshelveCommand.HelpDescription"); //$NON-NLS-1$
    }

    @Override
    public int run()
        throws Exception
    {
        verifyGitTfConfigured();
        verifyRepoSafeState();

        String name = ((FreeArgument) getArguments().getArgument("name")).getValue(); //$NON-NLS-1$

        String user = getArguments().contains("user") ? ((ValueArgument) getArguments().getArgument("user")).getValue() //$NON-NLS-1$ //$NON-NLS-2$
            : getConnection().getAuthenticatedIdentity().getUniqueName();

        boolean apply = getArguments().contains("apply"); //$NON-NLS-1$

        final UnshelveTask unshelveTask = new UnshelveTask(getVersionControlService(), getRepository(), name, user);
        unshelveTask.setApply(apply);

        final TaskStatus unshelveTaskResult = new CommandTaskExecutor(getProgressMonitor()).execute(unshelveTask);

        return unshelveTaskResult.isOK() ? ExitCode.SUCCESS : ExitCode.FAILURE;
    }

}
