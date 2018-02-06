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

package com.microsoft.gittf.client.clc.arguments.parser;

import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.arguments.*;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Test cases to verify the argument parser logic
 */
@SuppressWarnings("nls")
public class ArgumentParserTest
        extends TestCase {
    private static Argument[] TEST_ARGUMENTS = new Argument[]
            {
                    new SwitchArgument("switch-a", 'a', "switch a"),
                    new SwitchArgument("switch-b", 'b', "switch b"),
                    new SwitchArgument("switch-c", 'c', "switch c"),

                    new ValueArgument("value-one", '1', "value one", "value one", ArgumentOptions.VALUE_REQUIRED),
                    new ValueArgument("value-two", '2', "value two", "value two", ArgumentOptions.VALUE_REQUIRED),
                    new ValueArgument("value-three", '3', "value three", "value three"),
                    new ValueArgument("value-four", '4', "value four", "value four", ArgumentOptions.VALUE_REQUIRED),
                    new ValueArgument("value-five", '5', "value five", "value five", ArgumentOptions.VALUE_REQUIRED),
                    new ValueArgument("value-six", '6', "value six", "value six", ArgumentOptions.VALUE_REQUIRED),

                    new ValueArgument("multiple",
                            "multiple args",
                            "multiple args",
                            ArgumentOptions.VALUE_REQUIRED.combine(ArgumentOptions.MULTIPLE)),

                    new ChoiceArgument(Messages.getString("choice"),

                            new SwitchArgument("choice-1", "choice 1"),

                            new ValueArgument("choice-2", "choice 2", "choice 2", ArgumentOptions.VALUE_REQUIRED),

                            new SwitchArgument("choice-3", "choice 3")),

                    new FreeArgument("free-1", "free one"),

                    new FreeArgument("free-2", "free two"),
            };

    public void testAliasValue()
            throws Exception {
        ArgumentCollection args = ArgumentParser.parse(new String[]
                {
                        "-1",
                        "Argument Value is test Alias Value 1",
                        "-2=Argument Value is test Alias Value 2",
                        "-3=Argument Value is test Alias Value 3",
                        "-4Argument4",
                        "-5Argument Value in test Alias Value 5",
                        "-a6Argument Value in test Alias Value 6",
                }, TEST_ARGUMENTS);

        assertTrue(args.contains("value-one"));
        assertTrue(args.contains("value-two"));
        assertTrue(args.contains("value-three"));
        assertTrue(args.contains("value-four"));
        assertTrue(args.contains("value-five"));
        assertTrue(args.contains("value-six"));

        String v1 = ((ValueArgument) args.getArgument("value-one")).getValue();
        assertEquals(v1, "Argument Value is test Alias Value 1");

        String v2 = ((ValueArgument) args.getArgument("value-two")).getValue();
        assertEquals(v2, "Argument Value is test Alias Value 2");

        String v3 = ((ValueArgument) args.getArgument("value-three")).getValue();
        assertEquals(v3, "Argument Value is test Alias Value 3");

        String v4 = ((ValueArgument) args.getArgument("value-four")).getValue();
        assertEquals(v4, "Argument4");

        String v5 = ((ValueArgument) args.getArgument("value-five")).getValue();
        assertEquals(v5, "Argument Value in test Alias Value 5");

        String v6 = ((ValueArgument) args.getArgument("value-six")).getValue();
        assertEquals(v6, "Argument Value in test Alias Value 6");
    }

    public void testCaseSensitive()
            throws Exception {
        try {
            ArgumentParser.parse(new String[]
                            {
                                    "--SWITCH-A"
                            },
                    TEST_ARGUMENTS);

            throw new AssertionFailedError("expected case sensitive parsing");
        } catch (ArgumentParserException e) {
        }
    }

    public void testSwitchAbbreviation()
            throws Exception {
        ArgumentCollection abbrev = ArgumentParser.parse(new String[]
                {
                        "-a", "-b"
                }, TEST_ARGUMENTS);

        ArgumentCollection full = ArgumentParser.parse(new String[]
                {
                        "--switch-a", "--switch-b"
                }, TEST_ARGUMENTS);

        assertEquals(abbrev, full);
    }

    public void testSwitchCombination()
            throws Exception {
        ArgumentCollection args = ArgumentParser.parse(new String[]
                        {
                                "-abc"
                        },
                TEST_ARGUMENTS);

        assertTrue(args.contains("switch-a"));
        assertTrue(args.contains("switch-b"));
        assertTrue(args.contains("switch-c"));
    }

    public void testSwitchWithValue()
            throws Exception {
        try {
            ArgumentParser.parse(new String[]
                            {
                                    "--switch-a=foo"
                            },
                    TEST_ARGUMENTS);

            throw new AssertionFailedError("switches cannot have values");
        } catch (ArgumentParserException e) {
        }
    }

    public void testMultipleArguments()
            throws Exception {
        ArgumentCollection args = ArgumentParser.parse(new String[]
                {
                        "--multiple=one", "--multiple=two"
                }, TEST_ARGUMENTS);

        assertTrue(args.contains("multiple"));
        assertTrue(args.getArguments("multiple").length == 2);
        assertTrue(((ValueArgument) args.getArguments("multiple")[0]).getValue().equals("one"));
        assertTrue(((ValueArgument) args.getArguments("multiple")[1]).getValue().equals("two"));

        args = ArgumentParser.parse(new String[]
                        {
                                "--multiple=one,two"
                        },
                TEST_ARGUMENTS);

        assertTrue(args.contains("multiple"));
        assertTrue(args.getArguments("multiple").length == 2);
        assertTrue(((ValueArgument) args.getArguments("multiple")[0]).getValue().equals("one"));
        assertTrue(((ValueArgument) args.getArguments("multiple")[1]).getValue().equals("two"));

        args = ArgumentParser.parse(new String[]
                        {
                                "--multiple=one;two"
                        },
                TEST_ARGUMENTS);

        assertTrue(args.contains("multiple"));
        assertTrue(args.getArguments("multiple").length == 2);
        assertTrue(((ValueArgument) args.getArguments("multiple")[0]).getValue().equals("one"));
        assertTrue(((ValueArgument) args.getArguments("multiple")[1]).getValue().equals("two"));

        args = ArgumentParser.parse(new String[]
                {
                        "--value-one=one", "--value-one=two"
                }, TEST_ARGUMENTS);

        assertTrue(args.getArguments("value-one").length == 1);
        assertTrue(((ValueArgument) args.getArgument("value-one")).getValue().equals("two"));

        args = ArgumentParser.parse(new String[]
                        {
                                "--value-one=one,two"
                        },
                TEST_ARGUMENTS);

        assertTrue(args.getArguments("value-one").length == 1);
        assertTrue(((ValueArgument) args.getArgument("value-one")).getValue().equals("one,two"));
    }

    public void testChoiceLimitation()
            throws Exception {
        ArgumentCollection args = ArgumentParser.parse(new String[]
                        {
                                "--choice-1"
                        },
                TEST_ARGUMENTS);

        assertTrue(args.contains("choice-1"));

        try {
            ArgumentParser.parse(new String[]
                    {
                            "--choice-1", "--choice-2"
                    }, TEST_ARGUMENTS);

            throw new AssertionFailedError("multiple choice arguments");
        } catch (ArgumentParserException e) {
        }

        try {
            ArgumentParser.parse(new String[]
                    {
                            "--choice-1", "--choice-3"
                    }, TEST_ARGUMENTS);

            throw new AssertionFailedError("multiple choice arguments");
        } catch (ArgumentParserException e) {
        }

        try {
            ArgumentParser.parse(new String[]
                    {
                            "--choice-1", "--choice-2", "--choice-3"
                    }, TEST_ARGUMENTS);

            throw new AssertionFailedError("multiple choice arguments");
        } catch (ArgumentParserException e) {
        }
    }

    public void testValueRequired()
            throws Exception {
        ArgumentCollection args = ArgumentParser.parse(new String[]
                {
                        "--value-one", "value-one-data"
                }, TEST_ARGUMENTS);

        assertEquals(((ValueArgument) args.getArgument("value-one")).getValue(), "value-one-data");

        // Ensure that --value-one requires an argument
        try {
            ArgumentParser.parse(new String[]
                    {
                            "--choice-1", "--value-one"
                    }, TEST_ARGUMENTS);

            throw new AssertionFailedError("no value");
        } catch (ArgumentParserException e) {
        }

        // Ensure that --value-three isn't used as an value to --value-one
        try {
            ArgumentParser.parse(new String[]
                    {
                            "--value-one", "--value-three"
                    }, TEST_ARGUMENTS);

            throw new AssertionFailedError("no value");
        } catch (ArgumentParserException e) {
        }
    }

    public void testValueNotRequired()
            throws Exception {
        ArgumentCollection args = ArgumentParser.parse(new String[]
                {
                        "--value-one", "value-one-data", "--value-three", "value-three-data"
                }, TEST_ARGUMENTS);

        assertEquals(((ValueArgument) args.getArgument("value-one")).getValue(), "value-one-data");
        assertEquals(((ValueArgument) args.getArgument("value-three")).getValue(), "value-three-data");

        args = ArgumentParser.parse(new String[]
                {
                        "--value-one", "value-one-data", "--value-three"
                }, TEST_ARGUMENTS);

        assertEquals(((ValueArgument) args.getArgument("value-one")).getValue(), "value-one-data");
        assertEquals(((ValueArgument) args.getArgument("value-three")).getValue(), "");

        args = ArgumentParser.parse(new String[]
                {
                        "--value-one", "value-one-data", "--value-three", ""
                }, TEST_ARGUMENTS);

        assertEquals(((ValueArgument) args.getArgument("value-one")).getValue(), "value-one-data");
        assertEquals(((ValueArgument) args.getArgument("value-three")).getValue(), "");
    }

    public void testFreeArgument()
            throws Exception {
        ArgumentCollection args = ArgumentParser.parse(new String[]
                {
                        "--choice-1", "free-value"
                }, TEST_ARGUMENTS);

        assertTrue(args.contains("choice-1"));
        assertEquals(((FreeArgument) args.getArgument("free-1")).getValue(), "free-value");

        args = ArgumentParser.parse(new String[]
                {
                        "--choice-1", "free-value", "second-free-value"
                }, TEST_ARGUMENTS);

        assertTrue(args.contains("choice-1"));
        assertEquals(((FreeArgument) args.getArgument("free-1")).getValue(), "free-value");
        assertEquals(((FreeArgument) args.getArgument("free-2")).getValue(), "second-free-value");
    }

    public void testRequiredFreeArgument()
            throws Exception {
        Argument[] requiredFreeArgument = new Argument[]
                {
                        new FreeArgument("required-one", "required one", ArgumentOptions.REQUIRED),
                        new FreeArgument("required-two", "required two", ArgumentOptions.REQUIRED),
                        new FreeArgument("not-required-three", "not required three"),
                };

        ArgumentCollection args = ArgumentParser.parse(new String[]
                {
                        "required-value-one", "required-value-two", "value-three"
                }, requiredFreeArgument);

        assertEquals(((FreeArgument) args.getArgument("required-one")).getValue(), "required-value-one");
        assertEquals(((FreeArgument) args.getArgument("required-two")).getValue(), "required-value-two");
        assertEquals(((FreeArgument) args.getArgument("not-required-three")).getValue(), "value-three");

        args = ArgumentParser.parse(new String[]
                {
                        "required-value-one", "required-value-two"
                }, requiredFreeArgument);

        assertEquals(((FreeArgument) args.getArgument("required-one")).getValue(), "required-value-one");
        assertEquals(((FreeArgument) args.getArgument("required-two")).getValue(), "required-value-two");

        try {
            args = ArgumentParser.parse(new String[]
                    {
                            "required-value-one", ""
                    }, requiredFreeArgument);

            throw new AssertionFailedError("no value");
        } catch (ArgumentParserException e) {
        }
    }

    public void testTwoStageParsing()
            throws Exception {
        Argument[] stageOne = new Argument[]
                {
                        new SwitchArgument("stage-one", "stage one"), new FreeArgument("command", "command"),
                };

        ArgumentCollection stageOneArgs = ArgumentParser.parse(new String[]
                {
                        "--stage-one", "test-command", "--other-argument"
                }, stageOne, ArgumentParserOptions.ALLOW_UNKNOWN_ARGUMENTS);

        assertTrue(stageOneArgs.contains("stage-one"));
        assertTrue(stageOneArgs.contains("command"));
        assertTrue(((FreeArgument) stageOneArgs.getArgument("command")).getValue().equals("test-command"));
        assertTrue(stageOneArgs.getUnknownArguments().length == 1);
        assertTrue(stageOneArgs.getUnknownArguments()[0].equals("--other-argument"));
    }
}
