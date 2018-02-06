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

package com.microsoft.gittf.client.clc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.gittf.client.clc.Console.Verbosity;
import com.microsoft.gittf.client.clc.arguments.Argument;
import com.microsoft.gittf.client.clc.arguments.ArgumentOptions;
import com.microsoft.gittf.client.clc.arguments.ChoiceArgument;
import com.microsoft.gittf.client.clc.arguments.FreeArgumentCollection;
import com.microsoft.gittf.client.clc.arguments.SwitchArgument;
import com.microsoft.gittf.client.clc.arguments.parser.ArgumentCollection;
import com.microsoft.gittf.client.clc.arguments.parser.ArgumentParser;
import com.microsoft.gittf.client.clc.arguments.parser.ArgumentParserException;
import com.microsoft.gittf.client.clc.arguments.parser.ArgumentParserOptions;
import com.microsoft.gittf.client.clc.commands.CheckinCommand;
import com.microsoft.gittf.client.clc.commands.CloneCommand;
import com.microsoft.gittf.client.clc.commands.ConfigureCommand;
import com.microsoft.gittf.client.clc.commands.FetchCommand;
import com.microsoft.gittf.client.clc.commands.HelpCommand;
import com.microsoft.gittf.client.clc.commands.PullCommand;
import com.microsoft.gittf.client.clc.commands.ShelveCommand;
import com.microsoft.gittf.client.clc.commands.ShelvesetsCommand;
import com.microsoft.gittf.client.clc.commands.UnshelveCommand;
import com.microsoft.gittf.client.clc.commands.framework.Command;
import com.microsoft.gittf.client.clc.util.HelpFormatter;
import com.microsoft.gittf.client.clc.util.logging.LoggingConfiguration;

/**
 * Main class
 * 
 */
public class Main
{
    private static final Console console = new Console();

    /**
     * All commands that may be invoked.
     */
    public static final CommandDefinition[] COMMANDS = new CommandDefinition[]
    {
        new CommandDefinition(
            HelpCommand.COMMAND_NAME,
            HelpCommand.class,
            Messages.getString("Main.Command.Help.HelpText")), //$NON-NLS-1$

        new CommandDefinition(
            CloneCommand.COMMAND_NAME,
            CloneCommand.class,
            Messages.getString("Main.Command.Clone.HelpText")), //$NON-NLS-1$

        new CommandDefinition(
            ConfigureCommand.COMMAND_NAME,
            ConfigureCommand.class,
            Messages.getString("Main.Command.Configure.HelpText")), //$NON-NLS-1$            

        new CommandDefinition(
            CheckinCommand.COMMAND_NAME,
            CheckinCommand.class,
            Messages.getString("Main.Command.Checkin.HelpText")), //$NON-NLS-1$

        new CommandDefinition(
            FetchCommand.COMMAND_NAME,
            FetchCommand.class,
            Messages.getString("Main.Command.Fetch.HelpText")), //$NON-NLS-1$

        new CommandDefinition(
            PullCommand.COMMAND_NAME,
            PullCommand.class,
            Messages.getString("Main.Command.Pull.HelpText")), //$NON-NLS-1$

        new CommandDefinition(
            ShelveCommand.COMMAND_NAME,
            ShelveCommand.class,
            Messages.getString("Main.Command.Shelve.HelpText")), //$NON-NLS-1$

        new CommandDefinition(
            ShelvesetsCommand.COMMAND_NAME,
            ShelvesetsCommand.class,
            Messages.getString("Main.Command.Shelvesets.HelpText")), //$NON-NLS-1$

        new CommandDefinition(
            UnshelveCommand.COMMAND_NAME,
            UnshelveCommand.class,
            Messages.getString("Main.Command.Unshelve.HelpText")), //$NON-NLS-1$            
    };

    /**
     * All arguments that are accepted.
     */
    private static final Argument[] ARGUMENTS = new Argument[]
    {
        new SwitchArgument("version", //$NON-NLS-1$
            Messages.formatString("Main.Argument.Version.HelpTextFormat", ProductInformation.getProductName())), //$NON-NLS-1$

        new SwitchArgument("help", //$NON-NLS-1$
            Messages.getString("Command.Argument.Help.HelpText")), //$NON-NLS-1$

        new ChoiceArgument(Messages.getString("Command.Argument.Display.HelpText"), //$NON-NLS-1$
            new SwitchArgument("quiet", //$NON-NLS-1$
                'q',
                Messages.getString("Command.Argument.Quiet.HelpText")), //$NON-NLS-1$

            new SwitchArgument("verbose", //$NON-NLS-1$
                Messages.getString("Command.Argument.Verbose.HelpText")) //$NON-NLS-1$
        ),

        /*
         * The first free argument should be the command, remaining free
         * arguments should be the arguments for that command.
         */
        new FreeArgumentCollection("command", //$NON-NLS-1$
            Messages.getString("Main.Argument.Command.HelpText"), //$NON-NLS-1$
            ArgumentOptions.LITERAL),
    };

    public static void main(String[] args)
    {
        // Configure logging, use the standard TFS SDK logging.
        System.setProperty("teamexplorer.application", ProductInformation.getProductName()); //$NON-NLS-1$
        LoggingConfiguration.configure();

        final Log log = LogFactory.getLog(ProductInformation.getProductName());

        try
        {
            ArgumentCollection mainArguments = new ArgumentCollection();

            try
            {
                mainArguments = ArgumentParser.parse(args, ARGUMENTS, ArgumentParserOptions.ALLOW_UNKNOWN_ARGUMENTS);
            }
            catch (ArgumentParserException e)
            {
                console.getErrorStream().println(e.getLocalizedMessage());
                console.getErrorStream().println(getUsage());
                System.exit(ExitCode.FAILURE);
            }

            if (mainArguments.contains("version")) //$NON-NLS-1$
            {
                console.getOutputStream().println(Messages.formatString("Main.ApplicationVersionFormat", //$NON-NLS-1$
                    ProductInformation.getProductName(),
                    ProductInformation.getBuildNumber()));

                return;
            }

            /*
             * Special case "--help command" handling - convert to
             * "help command"
             */
            if (mainArguments.contains("help") && mainArguments.contains("command")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                HelpCommand helpCommand = new HelpCommand();
                helpCommand.setArguments(ArgumentParser.parse(new String[]
                {
                    ((FreeArgumentCollection) mainArguments.getArgument("command")).getValues()[0] //$NON-NLS-1$                
                    },
                    helpCommand.getPossibleArguments()));

                helpCommand.setConsole(console);
                helpCommand.run();
                return;
            }
            else if (mainArguments.contains("help") || !mainArguments.contains("command")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                showHelp();
                return;
            }

            // Set the verbosity of the console from the arguments.
            if (mainArguments.contains("quiet")) //$NON-NLS-1$
            {
                console.setVerbosity(Verbosity.QUIET);
            }
            else if (mainArguments.contains("verbose")) //$NON-NLS-1$
            {
                console.setVerbosity(Verbosity.VERBOSE);
            }

            /*
             * Parse the free arguments into the command name and arguments to
             * pass to it. Add any unmatched arguments that were specified on
             * the command line before the argument. (eg, for
             * "git-tf --bare clone", we parsed the "--bare" as an unmatched
             * argument to the main command. We instead want to add the "--bare"
             * as an argument to "clone".)
             */
            String[] fullCommand = ((FreeArgumentCollection) mainArguments.getArgument("command")).getValues(); //$NON-NLS-1$
            String[] additionalArguments = mainArguments.getUnknownArguments();

            String commandName = fullCommand[0];
            String[] commandArgs = new String[additionalArguments.length + (fullCommand.length - 1)];

            if (additionalArguments.length > 0)
            {
                System.arraycopy(additionalArguments, 0, commandArgs, 0, additionalArguments.length);
            }

            if (fullCommand.length > 1)
            {
                System.arraycopy(
                    fullCommand,
                    1,
                    commandArgs,
                    mainArguments.getUnknownArguments().length,
                    fullCommand.length - 1);
            }

            // Locate the specified command by name
            List<CommandDefinition> possibleCommands = new ArrayList<CommandDefinition>();

            for (CommandDefinition c : COMMANDS)
            {
                if (c.getName().equals(commandName))
                {
                    possibleCommands.clear();
                    possibleCommands.add(c);
                    break;
                }
                else if (c.getName().startsWith(commandName))
                {
                    possibleCommands.add(c);
                }
            }

            if (possibleCommands.size() == 0)
            {
                printError(Messages.formatString(
                    "Main.CommandNotFoundFormat", commandName, ProductInformation.getProductName())); //$NON-NLS-1$
                System.exit(1);
            }

            if (possibleCommands.size() > 1)
            {
                printError(Messages.formatString(
                    "Main.AmbiguousCommandFormat", commandName, ProductInformation.getProductName())); //$NON-NLS-1$

                for (CommandDefinition c : possibleCommands)
                {
                    printError(Messages.formatString("Main.AmbiguousCommandListFormat", c.getName()), false); //$NON-NLS-1$
                }

                System.exit(1);
            }

            // Instantiate the command
            final CommandDefinition commandDefinition = possibleCommands.get(0);
            Command command = null;

            try
            {
                command = commandDefinition.getType().newInstance();
            }
            catch (Exception e)
            {
                printError(Messages.formatString("Main.CommandCreationFailedFormat", commandName)); //$NON-NLS-1$
                System.exit(1);
            }

            // Set the console
            command.setConsole(console);

            // Parse the arguments
            ArgumentCollection argumentCollection = null;

            try
            {
                argumentCollection = ArgumentParser.parse(commandArgs, command.getPossibleArguments());
            }
            catch (ArgumentParserException e)
            {
                Main.printError(e.getLocalizedMessage());
                Main.printError(getUsage(command));

                log.error("Could not parse arguments", e); //$NON-NLS-1$
                System.exit(1);
            }

            // Handle the --help argument directly
            if (argumentCollection.contains("help")) //$NON-NLS-1$
            {
                command.showHelp();
                System.exit(0);
            }

            // Set the verbosity of the console from the arguments.
            if (argumentCollection.contains("quiet")) //$NON-NLS-1$
            {
                console.setVerbosity(Verbosity.QUIET);
            }
            else if (argumentCollection.contains("verbose")) //$NON-NLS-1$
            {
                console.setVerbosity(Verbosity.VERBOSE);
            }

            command.setArguments(argumentCollection);

            System.exit(command.run());

        }
        catch (Exception e)
        {
            printError(e.getLocalizedMessage());
            log.warn(MessageFormat.format("Error executing command: {0}", getCommandLine(args)), e); //$NON-NLS-1$

            System.exit(ExitCode.FAILURE);
        }
    }

    public static void showHelp()
    {
        console.getOutputStream().println(getUsage());
        console.getOutputStream().println();

        console.getOutputStream().println(
            Messages.formatString("Main.HelpCommandsFormat", ProductInformation.getProductName())); //$NON-NLS-1$

        for (CommandDefinition command : COMMANDS)
        {
            console.getOutputStream().println(MessageFormat.format("   {0} {1}", //$NON-NLS-1$
                String.format("%-10s", command.getName()), //$NON-NLS-1$
                command.getHelpText()));
        }
    }

    public static void printWarning(String warningMessage)
    {
        String message = Messages.formatString("Main.WarningFormat", warningMessage); //$NON-NLS-1$
        printError(message, false);
    }

    public static void printError(String message)
    {
        printError(message, true);
    }

    public static void printError(String message, boolean addApplicationName)
    {
        if (addApplicationName)
        {
            console.getErrorStream().println(
                MessageFormat.format("{0}: {1}", ProductInformation.getProductName(), message)); //$NON-NLS-1$
        }
        else
        {
            console.getErrorStream().println(message);
        }
    }

    private static String getCommandLine(String[] args)
    {
        StringBuilder s = new StringBuilder();

        for (String arg : args)
        {
            if (s.length() > 0)
            {
                s.append(' ');
            }

            s.append(arg);
        }

        return s.toString();
    }

    private static String getUsage()
    {
        return Messages.formatString("Main.UsageFormat", //$NON-NLS-1$
            ProductInformation.getProductName(),
            HelpFormatter.getArgumentSyntax(ARGUMENTS));
    }

    private static String getUsage(Command command)
    {
        return Messages.formatString("Main.UsageFormat", //$NON-NLS-1$
            ProductInformation.getProductName(),
            HelpFormatter.getArgumentSyntax(command.getPossibleArguments()));
    }

}
