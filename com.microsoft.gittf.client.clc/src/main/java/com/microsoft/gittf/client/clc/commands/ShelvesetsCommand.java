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
import com.microsoft.gittf.client.clc.Main;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.arguments.*;
import com.microsoft.gittf.client.clc.commands.framework.Command;
import com.microsoft.gittf.client.clc.commands.framework.CommandTaskExecutor;
import com.microsoft.gittf.client.clc.commands.framework.ShelvesetConsoleView;
import com.microsoft.gittf.core.tasks.ShelvesetDeleteTask;
import com.microsoft.gittf.core.tasks.ShelvesetsDisplayTask;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.shelveset.ShelvesetSortOption;

public class ShelvesetsCommand
        extends Command {
    public static final String COMMAND_NAME = "shelvesets";

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

                    new ValueArgument("user",
                            'u',
                            Messages.getString("ShelvesetsCommand.Argument.User.ValueDescription"),
                            Messages.getString("ShelvesetsCommand.Argument.User.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new ValueArgument("sort",
                            's',
                            Messages.getString("ShelvesetsCommand.Argument.Sort.ValueDescription"),
                            Messages.getString("ShelvesetsCommand.Argument.Sort.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new SwitchArgument("details", Messages.getString("ShelvesetsCommand.Argument.Details.HelpText")),

                    new SwitchArgument("delete", Messages.getString("ShelvesetsCommand.Argument.Delete.HelpText")),

                    new FreeArgument("name", Messages.getString("ShelvesetsCommand.Argument.Name.HelpText"))

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
        return Messages.getString("ShelvesetsCommand.HelpDescription");
    }

    @Override
    public int run()
            throws Exception {
        verifyGitTfConfigured();

        boolean delete = getArguments().contains("delete");

        String name =
                getArguments().contains("name") ? ((FreeArgument) getArguments().getArgument("name")).getValue() : null;

        String user = getArguments().contains("user") ? ((ValueArgument) getArguments().getArgument("user")).getValue()
                : getConnection().getAuthenticatedIdentity().getUniqueName();
        user = user.equals("*") ? null : user;

        if (delete) {
            // delete shelveset

            if (getArguments().contains("sort"))
            {
                Main.printWarning(Messages.getString("ShelvesetsCommand.SortWillBeIgnoredDeleteSpecified"));
            }

            if (getArguments().contains("details"))
            {
                Main.printWarning(Messages.getString("ShelvesetsCommand.DetailsWillBeIgnoredDeleteSpecified"));
            }

            if (!getArguments().contains("name"))
            {
                throw new Exception(Messages.getString("ShelvesetsCommand.DeleteNotSupportedWithoutName"));
            }

            final TaskStatus shelvesetsDeleteTaskResult =
                    new CommandTaskExecutor(getProgressMonitor()).execute(new ShelvesetDeleteTask(
                            getVersionControlService(),
                            name,
                            user));

            return shelvesetsDeleteTaskResult.isOK() ? ExitCode.SUCCESS : ExitCode.FAILURE;
        } else {
            // display shelveset(s)

            boolean displayShelvesetDetails = getArguments().contains("details");
            ShelvesetSortOption sortOption = getShelvesetSortOptionIfSpecified();

            ShelvesetConsoleView view = new ShelvesetConsoleView(console);

            final ShelvesetsDisplayTask shelvesetsDisplayTask =
                    new ShelvesetsDisplayTask(getVersionControlService(), view, name, user);

            shelvesetsDisplayTask.setDisplayDetails(displayShelvesetDetails);
            shelvesetsDisplayTask.setSortOption(sortOption);

            final TaskStatus shelvesetsDisplayTaskResult =
                    new CommandTaskExecutor(getProgressMonitor()).execute(shelvesetsDisplayTask);

            return shelvesetsDisplayTaskResult.isOK() ? ExitCode.SUCCESS : ExitCode.FAILURE;

        }
    }

    private ShelvesetSortOption getShelvesetSortOptionIfSpecified()
            throws Exception {
        String sortOption = getArguments().contains("sort") ?
                ((ValueArgument) getArguments().getArgument("sort")).getValue() : null;

        if (sortOption == null) {
            return ShelvesetSortOption.DATE;
        }

        try {
            return ShelvesetSortOption.valueOf(sortOption.toUpperCase());
        } catch (Exception e) {
            throw new Exception(Messages.formatString("ShelvesetsCommand.InvalidShelvetSortModeFormat", sortOption));
        }
    }

    @Override
    protected boolean isMultiRepositories() {
        return false;
    }
}
