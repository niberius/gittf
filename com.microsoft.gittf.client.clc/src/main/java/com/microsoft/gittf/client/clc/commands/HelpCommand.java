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

import com.microsoft.gittf.client.clc.*;
import com.microsoft.gittf.client.clc.arguments.Argument;
import com.microsoft.gittf.client.clc.arguments.FreeArgument;
import com.microsoft.gittf.client.clc.arguments.SwitchArgument;
import com.microsoft.gittf.client.clc.commands.framework.Command;

/**
 * Provides help for the given command.
 */
public class HelpCommand
        extends Command {
    public static final String COMMAND_NAME = "help";

    private static Argument[] ARGUMENTS = new Argument[]
            {
                    new SwitchArgument("help", Messages.getString("Command.Argument.Help.HelpText")),

                    new FreeArgument("command",
                            Messages.getString("HelpCommand.Argument.Command.HelpText")),
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
        return Messages.getString("HelpCommand.HelpDescription");
    }

    @Override
    public int run()
            throws Exception {
        if (!getArguments().contains("command"))
        {
            Main.showHelp();
            return ExitCode.SUCCESS;
        }

        String commandName = ((FreeArgument) getArguments().getArgument("command")).getValue();

        /* Locate the specified command by name */
        CommandDefinition commandDefinition = null;

        for (CommandDefinition c : Main.COMMANDS) {
            if (c.getName().equals(commandName)) {
                commandDefinition = c;
                break;
            }
        }

        if (commandDefinition == null) {
            Main.printError(Messages.formatString("HelpCommand.CommandNotFoundFormat",
                    commandName,
                    ProductInformation.getProductName()));

            return ExitCode.FAILURE;
        }

        /* Invoke the command's help */
        Command command = null;

        try {
            command = commandDefinition.getType().newInstance();
        } catch (Exception e) {
            Main.printError(Messages.formatString("Main.CommandCreationFailedFormat", commandName));
            Runtime.getRuntime().exit(1);
        }

        command.showHelp();

        return ExitCode.SUCCESS;
    }
}
