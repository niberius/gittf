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
import com.microsoft.gittf.core.tasks.UnshelveTask;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;

public class UnshelveCommand
        extends Command {
    public static final String COMMAND_NAME = "unshelve";

    private static Argument[] ARGUMENTS =
            new Argument[]
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

                            //new SwitchArgument("apply", 'a', Messages.getString("UnshelveCommand.Argument.Apply.HelpText")),

                            new ValueArgument("user",
                                    'u',
                                    Messages.getString("UnshelveCommand.Argument.User.ValueDescription"),
                                    Messages.getString("UnshelveCommand.Argument.User.HelpText"),
                                    ArgumentOptions.VALUE_REQUIRED),

                            new FreeArgument(
                                    "name", Messages.getString("UnshelveCommand.Argument.Name.HelpText"), ArgumentOptions.REQUIRED)
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
        return Messages.getString("UnshelveCommand.HelpDescription");
    }

    @Override
    public int run()
            throws Exception {
        verifyGitTfConfigured();
        verifyRepoSafeState();

        String name = ((FreeArgument) getArguments().getArgument("name")).getValue();

        String user = getArguments().contains("user") ? ((ValueArgument) getArguments().getArgument("user")).getValue()
                : getConnection().getAuthenticatedIdentity().getUniqueName();

        boolean apply = getArguments().contains("apply");

        final UnshelveTask unshelveTask = new UnshelveTask(getVersionControlService(), getRepository(), name, user);
        unshelveTask.setApply(apply);

        final TaskStatus unshelveTaskResult = new CommandTaskExecutor(getProgressMonitor()).execute(unshelveTask);

        return unshelveTaskResult.isOK() ? ExitCode.SUCCESS : ExitCode.FAILURE;
    }

}
