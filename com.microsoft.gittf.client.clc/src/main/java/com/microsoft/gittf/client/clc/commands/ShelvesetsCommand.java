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
import com.microsoft.gittf.client.clc.Main;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.arguments.Argument;
import com.microsoft.gittf.client.clc.arguments.ArgumentOptions;
import com.microsoft.gittf.client.clc.arguments.ChoiceArgument;
import com.microsoft.gittf.client.clc.arguments.FreeArgument;
import com.microsoft.gittf.client.clc.arguments.SwitchArgument;
import com.microsoft.gittf.client.clc.arguments.ValueArgument;
import com.microsoft.gittf.client.clc.commands.framework.Command;
import com.microsoft.gittf.client.clc.commands.framework.CommandTaskExecutor;
import com.microsoft.gittf.client.clc.commands.framework.ShelvesetConsoleView;
import com.microsoft.gittf.core.tasks.ShelvesetDeleteTask;
import com.microsoft.gittf.core.tasks.ShelvesetsDisplayTask;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.shelveset.ShelvesetSortOption;

public class ShelvesetsCommand
    extends Command
{
    public static final String COMMAND_NAME = "shelvesets"; //$NON-NLS-1$

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

        new ValueArgument("user", //$NON-NLS-1$
            'u',
            Messages.getString("ShelvesetsCommand.Argument.User.ValueDescription"), //$NON-NLS-1$
            Messages.getString("ShelvesetsCommand.Argument.User.HelpText"), //$NON-NLS-1$
            ArgumentOptions.VALUE_REQUIRED),

        new ValueArgument("sort", //$NON-NLS-1$
            's',
            Messages.getString("ShelvesetsCommand.Argument.Sort.ValueDescription"), //$NON-NLS-1$
            Messages.getString("ShelvesetsCommand.Argument.Sort.HelpText"), //$NON-NLS-1$
            ArgumentOptions.VALUE_REQUIRED),

        new SwitchArgument("details", Messages.getString("ShelvesetsCommand.Argument.Details.HelpText")), //$NON-NLS-1$ //$NON-NLS-2$

        new SwitchArgument("delete", Messages.getString("ShelvesetsCommand.Argument.Delete.HelpText")), //$NON-NLS-1$ //$NON-NLS-2$

        new FreeArgument("name", Messages.getString("ShelvesetsCommand.Argument.Name.HelpText")) //$NON-NLS-1$ //$NON-NLS-2$

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
        return Messages.getString("ShelvesetsCommand.HelpDescription"); //$NON-NLS-1$
    }

    @Override
    public int run()
        throws Exception
    {
        verifyGitTfConfigured();

        boolean delete = getArguments().contains("delete"); //$NON-NLS-1$

        String name =
            getArguments().contains("name") ? ((FreeArgument) getArguments().getArgument("name")).getValue() : null; //$NON-NLS-1$ //$NON-NLS-2$

        String user = getArguments().contains("user") ? ((ValueArgument) getArguments().getArgument("user")).getValue() //$NON-NLS-1$ //$NON-NLS-2$
            : getConnection().getAuthenticatedIdentity().getUniqueName();
        user = user.equals("*") ? null : user; //$NON-NLS-1$

        if (delete)
        {
            // delete shelveset

            if (getArguments().contains("sort")) //$NON-NLS-1$
            {
                Main.printWarning(Messages.getString("ShelvesetsCommand.SortWillBeIgnoredDeleteSpecified")); //$NON-NLS-1$
            }

            if (getArguments().contains("details")) //$NON-NLS-1$
            {
                Main.printWarning(Messages.getString("ShelvesetsCommand.DetailsWillBeIgnoredDeleteSpecified")); //$NON-NLS-1$
            }

            if (!getArguments().contains("name")) //$NON-NLS-1$
            {
                throw new Exception(Messages.getString("ShelvesetsCommand.DeleteNotSupportedWithoutName")); //$NON-NLS-1$
            }

            final TaskStatus shelvesetsDeleteTaskResult =
                new CommandTaskExecutor(getProgressMonitor()).execute(new ShelvesetDeleteTask(
                    getVersionControlService(),
                    name,
                    user));

            return shelvesetsDeleteTaskResult.isOK() ? ExitCode.SUCCESS : ExitCode.FAILURE;
        }
        else
        {
            // display shelveset(s)

            boolean displayShelvesetDetails = getArguments().contains("details"); //$NON-NLS-1$
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
        throws Exception
    {
        String sortOption = getArguments().contains("sort") ? //$NON-NLS-1$
            ((ValueArgument) getArguments().getArgument("sort")).getValue() : null; //$NON-NLS-1$

        if (sortOption == null)
        {
            return ShelvesetSortOption.DATE;
        }

        try
        {
            return ShelvesetSortOption.valueOf(sortOption.toUpperCase());
        }
        catch (Exception e)
        {
            throw new Exception(Messages.formatString("ShelvesetsCommand.InvalidShelvetSortModeFormat", sortOption)); //$NON-NLS-1$
        }
    }
}
