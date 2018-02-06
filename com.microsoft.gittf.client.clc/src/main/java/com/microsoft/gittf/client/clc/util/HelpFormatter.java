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

package com.microsoft.gittf.client.clc.util;

import com.microsoft.gittf.client.clc.arguments.*;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.jni.ConsoleUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Static utility class for formatting usage and help strings.
 */
public class HelpFormatter {
    private HelpFormatter() {
    }

    /**
     * Gets the argument syntax as would be used for short (single line) usage
     * information.
     *
     * @param arguments The command-line arguments accepted
     * @return A string representing the argument usage
     */
    public static String getArgumentSyntax(Argument[] arguments) {
        Check.notNull(arguments, "arguments");

        StringBuffer syntax = new StringBuffer();

        for (Argument argument : arguments) {
            if (argument.getOptions().contains(ArgumentOptions.HIDDEN)) {
                continue;
            }

            if (syntax.length() > 0) {
                syntax.append(" ");
            }

            if (!argument.getOptions().contains(ArgumentOptions.REQUIRED)) {
                syntax.append(MessageFormat.format("[{0}]", getArgumentSyntax(argument, "|")));
            } else {
                syntax.append(getArgumentSyntax(argument, "|"));
            }
        }

        return syntax.toString();
    }

    /**
     * Gets the argument syntax and description information as would be used for
     * multiple line help information.
     *
     * @param arguments The command-line arguments accepted
     * @return A string representing the help information for the arguments
     */
    public static String getArgumentHelp(Argument[] arguments) {
        StringBuffer help = new StringBuffer();

        for (Argument argument : expandArguments(arguments)) {
            if (argument.getOptions().contains(ArgumentOptions.HIDDEN)) {
                continue;
            }

            String argumentSyntax = getArgumentSyntax(argument, ", ");

            help.append("    ");
            help.append(String.format("%-21s", argumentSyntax));

            if (argumentSyntax.length() > 21) {
                help.append("\n");
                help.append("                          ");
            } else {
                help.append(" ");
            }

            help.append(wrap(argument.getHelpText(), 26));
            help.append("\n");
        }

        return help.toString();
    }

    private static List<Argument> expandArguments(Argument[] arguments) {
        final List<Argument> expanded = new ArrayList<Argument>();

        for (Argument argument : arguments) {
            /*
             * If a choice argument has help text of its own, it is to explain
             * all choices and we should flatten that.
             */
            if (argument instanceof ChoiceArgument && argument.getHelpText() == null) {
                expanded.addAll(expandArguments(((ChoiceArgument) argument).getArguments()));
            } else {
                expanded.add(argument);
            }
        }

        return expanded;
    }

    private static String getArgumentSyntax(Argument argument, String aliasSeparator) {
        StringBuffer syntax = new StringBuffer();

        if (argument instanceof ChoiceArgument) {
            Argument[] subArguments = ((ChoiceArgument) argument).getArguments();

            for (int i = 0; i < subArguments.length; i++) {
                if (i > 0) {
                    syntax.append(aliasSeparator);
                }

                syntax.append(getArgumentSyntax(subArguments[i], aliasSeparator));
            }
        } else if (argument instanceof NamedArgument) {
            syntax.append(MessageFormat.format("--{0}", argument.getName()));

            if (((NamedArgument) argument).getAlias() != (char) 0) {
                syntax.append(aliasSeparator);
                syntax.append(MessageFormat.format("-{0}", ((NamedArgument) argument).getAlias()));
            }

            if (argument instanceof ValueArgument) {
                syntax.append("=");

                if (!argument.getOptions().contains(ArgumentOptions.VALUE_REQUIRED)) {
                    syntax.append("[");
                }

                syntax.append(MessageFormat.format("<{0}>", ((ValueArgument) argument).getValueDescription()));

                if (!argument.getOptions().contains(ArgumentOptions.VALUE_REQUIRED)) {
                    syntax.append("]");
                }
            }
        } else if (argument instanceof LiteralArgument) {
            syntax.append("--");
        } else if (argument instanceof FreeArgument) {
            syntax.append(MessageFormat.format("<{0}>", argument.getName()));
        } else if (argument instanceof FreeArgumentCollection) {
            syntax.append(MessageFormat.format("<{0}...>", argument.getName()));
        }

        return syntax.toString();
    }

    public static String wrap(String paragraph) {
        return wrap(paragraph, 0);
    }

    private static String wrap(String paragraph, final int indent) {
        Check.notNullOrEmpty(paragraph, "paragraph");

        StringBuilder wrapped = new StringBuilder();

        int wrapWidth = (ConsoleUtils.getInstance().getConsoleColumns() - 2) - indent;
        if (wrapWidth < 0) {
            wrapWidth = 60;
        }

        while (paragraph.length() > wrapWidth) {
            /*
             * Pass one: check for a newline between us and the wrap width - if
             * there is one, we should advance to that line.
             */
            boolean modified = false;

            for (int i = 0; i < wrapWidth; i++) {
                if (paragraph.charAt(i) == '\n') {
                    wrapped.append(paragraph.substring(0, i + 1));

                    for (int j = 0; j < indent; j++) {
                        wrapped.append(' ');
                    }

                    paragraph = paragraph.substring(i + 1);
                    modified = true;
                    break;
                }
            }

            if (modified) {
                continue;
            }

            /*
             * Pass two: walk backwards from wrapping width, break on the first
             * whitespace.
             */
            for (int i = wrapWidth; i >= 0; i--) {
                if (Character.isWhitespace(paragraph.charAt(i))) {
                    wrapped.append(paragraph.substring(0, i));
                    wrapped.append('\n');

                    for (int j = 0; j < indent; j++) {
                        wrapped.append(' ');
                    }

                    paragraph = paragraph.substring(i + 1);
                    break;
                }
            }
        }

        if (paragraph.length() > 0) {
            wrapped.append(paragraph);
        }

        return wrapped.toString();
    }
}
