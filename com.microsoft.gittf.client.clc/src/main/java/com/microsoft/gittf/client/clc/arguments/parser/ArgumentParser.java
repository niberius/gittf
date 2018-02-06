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

package com.microsoft.gittf.client.clc.arguments.parser;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.arguments.Argument;
import com.microsoft.gittf.client.clc.arguments.ArgumentOptions;
import com.microsoft.gittf.client.clc.arguments.ChoiceArgument;
import com.microsoft.gittf.client.clc.arguments.FreeArgument;
import com.microsoft.gittf.client.clc.arguments.FreeArgumentCollection;
import com.microsoft.gittf.client.clc.arguments.LiteralArgument;
import com.microsoft.gittf.client.clc.arguments.NamedArgument;
import com.microsoft.gittf.client.clc.arguments.ValueArgument;
import com.microsoft.gittf.core.util.Check;

/**
 * Parses the given command line according to the given allowed {@link Argument}
 * s.
 * 
 * Argument parsing occurs as follows:
 * 
 * If an argument begins with "--" or '-', it is assumed to be a switch argument
 * or a value argument. Arguments in long form (using their full name) are
 * prefixed with two dashes ("--"), and may have a single character short form
 * alias prefixed with a single dash ('-'). For example, an argument may be
 * specified as "one" with an alias of 'f'. In that case, "--one" is equivalent
 * to '-f'.
 * 
 * Switch arguments are boolean in nature and need only be specified on the
 * command line to be present. For the switch argument "one", specifying "--one"
 * will set that argument.
 * 
 * Value arguments may permit a value after the argument. In long form, values
 * may be specified with a space delimiter, or an equals sign delimiter. For the
 * value argument "one", specifying "--one=bar" is equivalent to specifying
 * "--one bar". In short form, values must be specified with a space delimiter,
 * for example "-f bar".
 * 
 * Multiple switch arguments may be combined in short form using a single dash
 * ('-'). For example, if argument aliases 'a', 'b' and 'c' exist then the
 * command line "-abc" is equivalent to "-a -b -c".
 * 
 * A single value argument in short form may be combined with several switch
 * arguments in short form using a single dash. For example, if switch arguments
 * 'a' and 'b' exist and value argument 'c' exists then the command line
 * "-abc one" is equivalent to "-a -b -c one".
 * 
 * Multiple value arguments may not be combined in short form. For example, if
 * '-a' and '-b' are aliases for value arguments, the valid command line is
 * "-a one -b bar". A command line of "-ab one bar" is not valid.
 * 
 * Free arguments are those that are specified as lone arguments and not as the
 * value of a value argument. For example, given arguments "file1 file2", both
 * "file1" and "file2" are free arguments. Free arguments are matched by
 * position. Free arguments may be collected for variable length argument
 * matching.
 * 
 * If a literal argument specifier is allowed then an argument of two dashes
 * ("--") will indicate that any further command line arguments are to be
 * treated as free arguments, regardless of whether they are prefixed with a
 * '-'. For example, if the literal argument specifier is allowed then in the
 * command line "--one -- file1 file --file3", the argument "--file3" is treated
 * as a free argument, not a switch argument.
 * 
 */
public class ArgumentParser
{
    /**
     * Parses the given command line provided by the user and returns a
     * collection of {@link Argument}s that were specified.
     * 
     * @param commandLine
     *        The command line provided by the user (not <code>null</code>)
     * @param allowedArguments
     *        The list of {@link Argument}s that may be specified
     * @return An {@link ArgumentCollection} that contains all specified
     *         arguments (never <code>null</code>).
     * @throws ArgumentParserException
     *         if the command line was not valid or could not be parsed
     */
    public static ArgumentCollection parse(final String[] commandLine, final Argument[] allowedArguments)
        throws ArgumentParserException
    {
        return parse(commandLine, allowedArguments, ArgumentParserOptions.NONE);
    }

    /**
     * Parses the given command line provided by the user and returns a
     * collection of {@link Argument}s that were specified.
     * 
     * @param commandLine
     *        The command line provided by the user (not <code>null</code>)
     * @param allowedArguments
     *        The list of {@link Argument}s that may be specified (not
     *        <code>null</code>)
     * @param options
     *        {@link ArgumentParserOptions} to influence parsing behavior (not
     *        <code>null</code>)
     * @return An {@link ArgumentCollection} that contains all specified
     *         arguments (never <code>null</code>).
     * @throws ArgumentParserException
     *         if the command line was not valid or could not be parsed
     */
    public static ArgumentCollection parse(
        final String[] commandLine,
        final Argument[] allowedArguments,
        final ArgumentParserOptions options)
        throws ArgumentParserException
    {
        Check.notNull(commandLine, "args"); //$NON-NLS-1$
        Check.notNull(allowedArguments, "allowedArguments"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        List<Argument> allAllowedArguments = flattenArguments(allowedArguments);
        validateAllowedArguments(allAllowedArguments);

        ArgumentCollection arguments = new ArgumentCollection();

        int freeArgumentCount = 0;
        boolean nextArgumentLiteral = false;
        FreeArgumentCollection currentFreeArgumentCollection = null;

        for (int i = 0; i < commandLine.length; i++)
        {
            /*
             * Optionally allow "--" to specify that the remaining arguments
             * will be literal arguments.
             */
            if (commandLine[i].equals("--") && !nextArgumentLiteral) //$NON-NLS-1$
            {
                boolean allowsLiteral = false;

                for (Argument possibleArgument : allAllowedArguments)
                {
                    if (possibleArgument instanceof LiteralArgument)
                    {
                        allowsLiteral = true;
                        nextArgumentLiteral = true;
                        break;
                    }
                }

                if (!allowsLiteral)
                {
                    handleUnknownArgument(commandLine[i], arguments, options);
                }
            }
            /*
             * Handle named arguments in long form, beginning with '--'
             */
            else if (commandLine[i].startsWith("--") && !nextArgumentLiteral) //$NON-NLS-1$
            {
                String name = commandLine[i].substring(2);
                Argument argument = null;

                for (Argument possibleArgument : allAllowedArguments)
                {
                    if (possibleArgument instanceof NamedArgument && name.equals(possibleArgument.getName()))
                    {
                        argument = possibleArgument.clone();

                        if (argument instanceof ValueArgument)
                        {
                            /*
                             * Consume the next argument as the value for this
                             * one, unless it appears to be another argument.
                             */
                            if ((i + 1) >= commandLine.length || commandLine[i + 1].startsWith("-")) //$NON-NLS-1$
                            {
                                ((ValueArgument) argument).setValue(""); //$NON-NLS-1$
                            }
                            else
                            {
                                i++;
                                ((ValueArgument) argument).setValue(commandLine[i]);
                            }
                        }

                        break;
                    }
                }

                /* Try argument=value parsing */
                List<Argument> allValues = null;
                if (argument == null && name.contains("=")) //$NON-NLS-1$
                {
                    String[] nameValue = name.split("=", 2); //$NON-NLS-1$

                    for (Argument possibleArgument : allAllowedArguments)
                    {
                        if (possibleArgument instanceof ValueArgument
                            && nameValue[0].equals(possibleArgument.getName()))
                        {
                            /*
                             * Handle comma or semicolon separated multiple
                             * arguments
                             */
                            if (possibleArgument.getOptions().contains(ArgumentOptions.MULTIPLE))
                            {
                                String[] multipleValues = nameValue[1].split(",|;"); //$NON-NLS-1$

                                if (multipleValues.length <= 0)
                                {
                                    throw new ArgumentParserException(Messages.formatString(
                                        "ArgumentParser.ArgumentRequiresValueFormat", possibleArgument)); //$NON-NLS-1$
                                }

                                allValues = new ArrayList<Argument>();
                                for (String value : multipleValues)
                                {
                                    Argument valueArgument = possibleArgument.clone();
                                    ((ValueArgument) valueArgument).setValue(value);
                                    allValues.add(valueArgument);
                                }
                                break;
                            }
                            else
                            {
                                argument = possibleArgument.clone();
                                ((ValueArgument) argument).setValue(nameValue[1]);
                            }
                        }
                    }
                }

                if (argument == null && allValues == null)
                {
                    handleUnknownArgument(commandLine[i], arguments, options);
                }
                else if (argument != null)
                {
                    arguments.add(argument);
                }
                else
                {
                    for (Argument valueArgument : allValues)
                    {
                        arguments.add(valueArgument);
                    }
                }
            }
            /*
             * Handle named arguments in short (aliased) form, beginning with
             * '-'
             */
            else if (commandLine[i].startsWith("-") && !nextArgumentLiteral) //$NON-NLS-1$
            {
                String[] nameValue = null;
                if (commandLine[i].contains("=")) //$NON-NLS-1$
                {
                    nameValue = commandLine[i].split("=", 2); //$NON-NLS-1$
                }

                /*
                 * Multiple switches may be combined, eg "-asdf" ==
                 * "-a -s -d -f"
                 */
                char[] switches =
                    nameValue != null ? nameValue[0].substring(1).toCharArray()
                        : commandLine[i].substring(1).toCharArray();

                for (int j = 0; j < switches.length; j++)
                {
                    Argument argument = null;

                    for (Argument possibleArgument : allAllowedArguments)
                    {
                        if (possibleArgument instanceof NamedArgument)
                        {
                            if (((NamedArgument) possibleArgument).getAlias() == switches[j])
                            {
                                argument = possibleArgument.clone();
                                break;
                            }
                        }
                    }

                    if (argument == null)
                    {
                        handleUnknownArgument(MessageFormat.format("-{0}", switches[j]), arguments, options); //$NON-NLS-1$
                    }

                    /* The first switch may be a value argument */
                    if (j == 0 && nameValue == null && commandLine[i].length() > 2 && argument instanceof ValueArgument)
                    {
                        ((ValueArgument) argument).setValue(commandLine[i].substring(2));
                        arguments.add(argument);
                        break;
                    }
                    /* The last switch may be a value argument */
                    else if (j == switches.length - 1 && argument instanceof ValueArgument)
                    {
                        if (nameValue != null)
                        {
                            ((ValueArgument) argument).setValue(nameValue[1]);
                        }
                        /* Consume the next argument as the value for this one */
                        else
                        {
                            if ((i + 1) >= commandLine.length)
                            {
                                throw new ArgumentParserException(Messages.formatString(
                                    "ArgumentParser.ArgumentRequiresValueFormat", //$NON-NLS-1$
                                    MessageFormat.format("-{0}", switches[j]) //$NON-NLS-1$
                                ));
                            }

                            i++;
                            ((ValueArgument) argument).setValue(commandLine[i]);
                        }
                    }
                    else if (j < switches.length && argument instanceof ValueArgument)
                    {
                        // add 2 to make up for the - & the argument alias
                        ((ValueArgument) argument).setValue(commandLine[i].substring(j + 2));
                        arguments.add(argument);
                        break;
                    }
                    else if (argument instanceof ValueArgument)
                    {
                        throw new ArgumentParserException(Messages.formatString(
                            "ArgumentParser.ArgumentRequiresValueFormat", //$NON-NLS-1$
                            MessageFormat.format("-{0}", switches[j]) //$NON-NLS-1$
                        ));
                    }

                    arguments.add(argument);
                }
            }
            /*
             * If we are currently in a free argument collection, this argument
             * also applies.
             */
            else if (currentFreeArgumentCollection != null)
            {
                currentFreeArgumentCollection.addValue(commandLine[i]);
            }
            /*
             * Otherwise, see if this free argument will be accepted.
             */
            else
            {
                /* Find the next free argument */
                Argument argument = null;
                int freeArgumentIdx = 0;

                for (Argument possibleArgument : allAllowedArguments)
                {
                    if (possibleArgument instanceof FreeArgumentCollection)
                    {
                        argument = possibleArgument.clone();
                        currentFreeArgumentCollection = (FreeArgumentCollection) argument;

                        ((FreeArgumentCollection) argument).addValue(commandLine[i]);
                        arguments.add(argument);

                        if (currentFreeArgumentCollection.getOptions().contains(ArgumentOptions.LITERAL))
                        {
                            nextArgumentLiteral = true;
                        }

                        break;
                    }
                    else if (possibleArgument instanceof FreeArgument)
                    {
                        if (freeArgumentIdx == freeArgumentCount)
                        {
                            argument = possibleArgument.clone();

                            ((FreeArgument) argument).setValue(commandLine[i]);
                            arguments.add(argument);

                            freeArgumentCount++;

                            break;
                        }

                        freeArgumentIdx++;
                        nextArgumentLiteral = false;
                    }
                }

                if (argument == null)
                {
                    handleUnknownArgument(commandLine[i], arguments, options);
                }
            }
        }

        if (enforceRequirements(arguments))
        {
            enforceChoiceArguments(arguments, allowedArguments);
            enforceRequiredArguments(arguments, allowedArguments);
            enforceRequiredValueArguments(arguments, allowedArguments);
        }

        return arguments;
    }

    private static List<Argument> flattenArguments(Argument[] arguments)
    {
        List<Argument> flattened = new ArrayList<Argument>();

        for (Argument argument : arguments)
        {
            if (argument instanceof ChoiceArgument)
            {
                flattened.addAll(flattenArguments(((ChoiceArgument) argument).getArguments()));
            }
            else
            {
                flattened.add(argument);
            }
        }

        return flattened;
    }

    private static void validateAllowedArguments(List<Argument> arguments)
    {
        Set<String> names = new HashSet<String>();
        Set<Character> aliases = new HashSet<Character>();

        for (Argument argument : arguments)
        {
            /* Ensure this list has been flattened */
            Check.isTrue(!(argument instanceof ChoiceArgument), "argument != ChoiceArgument"); //$NON-NLS-1$

            if (argument.getName() == null)
            {
                continue;
            }

            Check.isTrue(names.add(argument.getName()), "names.add"); //$NON-NLS-1$

            if (argument instanceof NamedArgument && ((NamedArgument) argument).getAlias() != 0)
            {
                Check.isTrue(aliases.add(((NamedArgument) argument).getAlias()), "aliases.add"); //$NON-NLS-1$
            }
        }
    }

    private static void handleUnknownArgument(
        String unknownArg,
        ArgumentCollection arguments,
        ArgumentParserOptions options)
        throws ArgumentParserException
    {
        Check.notNull(unknownArg, "unknownArg"); //$NON-NLS-1$
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        if (options.contains(ArgumentParserOptions.ALLOW_UNKNOWN_ARGUMENTS))
        {
            arguments.addUnknownArgument(unknownArg);
        }
        else
        {
            throw new ArgumentParserException(Messages.formatString("ArgumentParser.UnknownArgumentFormat", unknownArg)); //$NON-NLS-1$
        }
    }

    private static boolean enforceRequirements(final ArgumentCollection specified)
    {
        Check.notNull(specified, "specified"); //$NON-NLS-1$

        for (Argument a : specified.getArguments())
        {
            if (a.getOptions().contains(ArgumentOptions.SUPPRESS_REQUIREMENTS))
            {
                return false;
            }
        }

        return true;
    }

    private static void enforceChoiceArguments(ArgumentCollection specified, Argument[] allowed)
        throws ArgumentParserException
    {
        Check.notNull(specified, "specified"); //$NON-NLS-1$
        Check.notNull(allowed, "allowed"); //$NON-NLS-1$

        for (Argument a : allowed)
        {
            if (a instanceof ChoiceArgument)
            {
                String existingName = null;
                List<Argument> allChoices = flattenArguments(((ChoiceArgument) a).getArguments());

                for (Argument choice : allChoices)
                {
                    if (specified.contains(choice.getName()))
                    {
                        if (existingName != null)
                        {
                            throw new ArgumentParserException(Messages.formatString(
                                "ArgumentParser.MultipleChoicesSpecifiedFormat", //$NON-NLS-1$
                                choice.getName(),
                                existingName));
                        }
                        else
                        {
                            existingName = choice.getName();
                        }
                    }
                }
            }
        }
    }

    private static void enforceRequiredArguments(ArgumentCollection specified, Argument[] allowed)
        throws ArgumentParserException
    {
        Check.notNull(specified, "specified"); //$NON-NLS-1$
        Check.notNull(allowed, "allowed"); //$NON-NLS-1$

        for (Argument a : allowed)
        {
            if (a.getOptions().contains(ArgumentOptions.REQUIRED))
            {
                if (!specified.contains(a.getName()))
                {
                    throw new ArgumentParserException(Messages.formatString(
                        "ArgumentParser.RequiredArgumentMissingFormat", //$NON-NLS-1$
                        a.getName()));
                }
            }
        }
    }

    private static void enforceRequiredValueArguments(ArgumentCollection specified, Argument[] allowed)
        throws ArgumentParserException
    {
        Check.notNull(specified, "specified"); //$NON-NLS-1$
        Check.notNull(allowed, "allowed"); //$NON-NLS-1$

        for (Argument a : specified.getArguments())
        {
            if (a.getOptions().contains(ArgumentOptions.VALUE_REQUIRED)
                && a instanceof ValueArgument
                && ((ValueArgument) a).getValue().equals("")) //$NON-NLS-1$
            {
                throw new ArgumentParserException(Messages.formatString("ArgumentParser.ArgumentRequiresValueFormat", //$NON-NLS-1$
                    a.getName()));
            }

            if (a.getOptions().contains(ArgumentOptions.REQUIRED)
                && a instanceof FreeArgument
                && ((FreeArgument) a).getValue().equals("")) //$NON-NLS-1$
            {
                throw new ArgumentParserException(Messages.formatString(
                    "ArgumentParser.FreeArgumentRequiresValueFormat", //$NON-NLS-1$
                    a.getName()));
            }
        }
    }
}
