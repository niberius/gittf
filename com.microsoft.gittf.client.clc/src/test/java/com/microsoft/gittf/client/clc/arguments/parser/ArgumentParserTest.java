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

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.arguments.Argument;
import com.microsoft.gittf.client.clc.arguments.ArgumentOptions;
import com.microsoft.gittf.client.clc.arguments.ChoiceArgument;
import com.microsoft.gittf.client.clc.arguments.FreeArgument;
import com.microsoft.gittf.client.clc.arguments.SwitchArgument;
import com.microsoft.gittf.client.clc.arguments.ValueArgument;

/**
 * Test cases to verify the argument parser logic
 * 
 */
@SuppressWarnings("nls")
public class ArgumentParserTest
    extends TestCase
{
    private static Argument[] TEST_ARGUMENTS = new Argument[]
    {
        new SwitchArgument("switch-a", 'a', "switch a"), //$NON-NLS-1$ //$NON-NLS-2$
        new SwitchArgument("switch-b", 'b', "switch b"), //$NON-NLS-1$ //$NON-NLS-2$
        new SwitchArgument("switch-c", 'c', "switch c"), //$NON-NLS-1$ //$NON-NLS-2$

        new ValueArgument("value-one", '1', "value one", "value one", ArgumentOptions.VALUE_REQUIRED), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        new ValueArgument("value-two", '2', "value two", "value two", ArgumentOptions.VALUE_REQUIRED), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        new ValueArgument("value-three", '3', "value three", "value three"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        new ValueArgument("value-four", '4', "value four", "value four", ArgumentOptions.VALUE_REQUIRED), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        new ValueArgument("value-five", '5', "value five", "value five", ArgumentOptions.VALUE_REQUIRED), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        new ValueArgument("value-six", '6', "value six", "value six", ArgumentOptions.VALUE_REQUIRED), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        new ValueArgument("multiple", //$NON-NLS-1$
            "multiple args", //$NON-NLS-1$
            "multiple args", //$NON-NLS-1$
            ArgumentOptions.VALUE_REQUIRED.combine(ArgumentOptions.MULTIPLE)),

        new ChoiceArgument(Messages.getString("choice"), //$NON-NLS-1$

            new SwitchArgument("choice-1", "choice 1"), //$NON-NLS-1$ //$NON-NLS-2$

            new ValueArgument("choice-2", "choice 2", "choice 2", ArgumentOptions.VALUE_REQUIRED), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            new SwitchArgument("choice-3", "choice 3")), //$NON-NLS-1$ //$NON-NLS-2$

        new FreeArgument("free-1", "free one"), //$NON-NLS-1$ //$NON-NLS-2$

        new FreeArgument("free-2", "free two"), //$NON-NLS-1$ //$NON-NLS-2$
    };

    public void testAliasValue()
        throws Exception
    {
        ArgumentCollection args = ArgumentParser.parse(new String[]
        {
            "-1", //$NON-NLS-1$
            "Argument Value is test Alias Value 1", //$NON-NLS-1$
            "-2=Argument Value is test Alias Value 2", //$NON-NLS-1$
            "-3=Argument Value is test Alias Value 3", //$NON-NLS-1$
            "-4Argument4", //$NON-NLS-1$
            "-5Argument Value in test Alias Value 5", //$NON-NLS-1$
            "-a6Argument Value in test Alias Value 6", //$NON-NLS-1$
        }, TEST_ARGUMENTS);

        assertTrue(args.contains("value-one")); //$NON-NLS-1$
        assertTrue(args.contains("value-two")); //$NON-NLS-1$
        assertTrue(args.contains("value-three")); //$NON-NLS-1$
        assertTrue(args.contains("value-four")); //$NON-NLS-1$
        assertTrue(args.contains("value-five")); //$NON-NLS-1$
        assertTrue(args.contains("value-six")); //$NON-NLS-1$

        String v1 = ((ValueArgument) args.getArgument("value-one")).getValue(); //$NON-NLS-1$
        assertEquals(v1, "Argument Value is test Alias Value 1"); //$NON-NLS-1$

        String v2 = ((ValueArgument) args.getArgument("value-two")).getValue(); //$NON-NLS-1$
        assertEquals(v2, "Argument Value is test Alias Value 2"); //$NON-NLS-1$

        String v3 = ((ValueArgument) args.getArgument("value-three")).getValue(); //$NON-NLS-1$
        assertEquals(v3, "Argument Value is test Alias Value 3"); //$NON-NLS-1$

        String v4 = ((ValueArgument) args.getArgument("value-four")).getValue(); //$NON-NLS-1$
        assertEquals(v4, "Argument4"); //$NON-NLS-1$

        String v5 = ((ValueArgument) args.getArgument("value-five")).getValue(); //$NON-NLS-1$
        assertEquals(v5, "Argument Value in test Alias Value 5"); //$NON-NLS-1$

        String v6 = ((ValueArgument) args.getArgument("value-six")).getValue(); //$NON-NLS-1$
        assertEquals(v6, "Argument Value in test Alias Value 6"); //$NON-NLS-1$
    }

    public void testCaseSensitive()
        throws Exception
    {
        try
        {
            ArgumentParser.parse(new String[]
            {
                "--SWITCH-A" //$NON-NLS-1$
            },
                TEST_ARGUMENTS);

            throw new AssertionFailedError("expected case sensitive parsing"); //$NON-NLS-1$
        }
        catch (ArgumentParserException e)
        {
        }
    }

    public void testSwitchAbbreviation()
        throws Exception
    {
        ArgumentCollection abbrev = ArgumentParser.parse(new String[]
        {
            "-a", "-b" //$NON-NLS-1$ //$NON-NLS-2$
        }, TEST_ARGUMENTS);

        ArgumentCollection full = ArgumentParser.parse(new String[]
        {
            "--switch-a", "--switch-b" //$NON-NLS-1$ //$NON-NLS-2$
        }, TEST_ARGUMENTS);

        assertEquals(abbrev, full);
    }

    public void testSwitchCombination()
        throws Exception
    {
        ArgumentCollection args = ArgumentParser.parse(new String[]
        {
            "-abc" //$NON-NLS-1$
        },
            TEST_ARGUMENTS);

        assertTrue(args.contains("switch-a")); //$NON-NLS-1$
        assertTrue(args.contains("switch-b")); //$NON-NLS-1$
        assertTrue(args.contains("switch-c")); //$NON-NLS-1$
    }

    public void testSwitchWithValue()
        throws Exception
    {
        try
        {
            ArgumentParser.parse(new String[]
            {
                "--switch-a=foo" //$NON-NLS-1$
            },
                TEST_ARGUMENTS);

            throw new AssertionFailedError("switches cannot have values"); //$NON-NLS-1$
        }
        catch (ArgumentParserException e)
        {
        }
    }

    public void testMultipleArguments()
        throws Exception
    {
        ArgumentCollection args = ArgumentParser.parse(new String[]
        {
            "--multiple=one", "--multiple=two" //$NON-NLS-1$ //$NON-NLS-2$
        }, TEST_ARGUMENTS);

        assertTrue(args.contains("multiple")); //$NON-NLS-1$
        assertTrue(args.getArguments("multiple").length == 2); //$NON-NLS-1$
        assertTrue(((ValueArgument) args.getArguments("multiple")[0]).getValue().equals("one")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(((ValueArgument) args.getArguments("multiple")[1]).getValue().equals("two")); //$NON-NLS-1$ //$NON-NLS-2$

        args = ArgumentParser.parse(new String[]
        {
            "--multiple=one,two" //$NON-NLS-1$
        },
            TEST_ARGUMENTS);

        assertTrue(args.contains("multiple")); //$NON-NLS-1$
        assertTrue(args.getArguments("multiple").length == 2); //$NON-NLS-1$
        assertTrue(((ValueArgument) args.getArguments("multiple")[0]).getValue().equals("one")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(((ValueArgument) args.getArguments("multiple")[1]).getValue().equals("two")); //$NON-NLS-1$ //$NON-NLS-2$

        args = ArgumentParser.parse(new String[]
        {
            "--multiple=one;two" //$NON-NLS-1$
        },
            TEST_ARGUMENTS);

        assertTrue(args.contains("multiple")); //$NON-NLS-1$
        assertTrue(args.getArguments("multiple").length == 2); //$NON-NLS-1$
        assertTrue(((ValueArgument) args.getArguments("multiple")[0]).getValue().equals("one")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(((ValueArgument) args.getArguments("multiple")[1]).getValue().equals("two")); //$NON-NLS-1$ //$NON-NLS-2$

        args = ArgumentParser.parse(new String[]
        {
            "--value-one=one", "--value-one=two" //$NON-NLS-1$ //$NON-NLS-2$
        }, TEST_ARGUMENTS);

        assertTrue(args.getArguments("value-one").length == 1); //$NON-NLS-1$
        assertTrue(((ValueArgument) args.getArgument("value-one")).getValue().equals("two")); //$NON-NLS-1$ //$NON-NLS-2$

        args = ArgumentParser.parse(new String[]
        {
            "--value-one=one,two" //$NON-NLS-1$
        },
            TEST_ARGUMENTS);

        assertTrue(args.getArguments("value-one").length == 1); //$NON-NLS-1$
        assertTrue(((ValueArgument) args.getArgument("value-one")).getValue().equals("one,two")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testChoiceLimitation()
        throws Exception
    {
        ArgumentCollection args = ArgumentParser.parse(new String[]
        {
            "--choice-1" //$NON-NLS-1$
        },
            TEST_ARGUMENTS);

        assertTrue(args.contains("choice-1")); //$NON-NLS-1$

        try
        {
            ArgumentParser.parse(new String[]
            {
                "--choice-1", "--choice-2" //$NON-NLS-1$ //$NON-NLS-2$
            }, TEST_ARGUMENTS);

            throw new AssertionFailedError("multiple choice arguments"); //$NON-NLS-1$
        }
        catch (ArgumentParserException e)
        {
        }

        try
        {
            ArgumentParser.parse(new String[]
            {
                "--choice-1", "--choice-3" //$NON-NLS-1$ //$NON-NLS-2$
            }, TEST_ARGUMENTS);

            throw new AssertionFailedError("multiple choice arguments"); //$NON-NLS-1$
        }
        catch (ArgumentParserException e)
        {
        }

        try
        {
            ArgumentParser.parse(new String[]
            {
                "--choice-1", "--choice-2", "--choice-3" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }, TEST_ARGUMENTS);

            throw new AssertionFailedError("multiple choice arguments"); //$NON-NLS-1$
        }
        catch (ArgumentParserException e)
        {
        }
    }

    public void testValueRequired()
        throws Exception
    {
        ArgumentCollection args = ArgumentParser.parse(new String[]
        {
            "--value-one", "value-one-data" //$NON-NLS-1$ //$NON-NLS-2$
        }, TEST_ARGUMENTS);

        assertEquals(((ValueArgument) args.getArgument("value-one")).getValue(), "value-one-data"); //$NON-NLS-1$ //$NON-NLS-2$

        // Ensure that --value-one requires an argument
        try
        {
            ArgumentParser.parse(new String[]
            {
                "--choice-1", "--value-one" //$NON-NLS-1$ //$NON-NLS-2$
            }, TEST_ARGUMENTS);

            throw new AssertionFailedError("no value"); //$NON-NLS-1$
        }
        catch (ArgumentParserException e)
        {
        }

        // Ensure that --value-three isn't used as an value to --value-one
        try
        {
            ArgumentParser.parse(new String[]
            {
                "--value-one", "--value-three" //$NON-NLS-1$ //$NON-NLS-2$
            }, TEST_ARGUMENTS);

            throw new AssertionFailedError("no value"); //$NON-NLS-1$
        }
        catch (ArgumentParserException e)
        {
        }
    }

    public void testValueNotRequired()
        throws Exception
    {
        ArgumentCollection args = ArgumentParser.parse(new String[]
        {
            "--value-one", "value-one-data", "--value-three", "value-three-data" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }, TEST_ARGUMENTS);

        assertEquals(((ValueArgument) args.getArgument("value-one")).getValue(), "value-one-data"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(((ValueArgument) args.getArgument("value-three")).getValue(), "value-three-data"); //$NON-NLS-1$ //$NON-NLS-2$

        args = ArgumentParser.parse(new String[]
        {
            "--value-one", "value-one-data", "--value-three" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }, TEST_ARGUMENTS);

        assertEquals(((ValueArgument) args.getArgument("value-one")).getValue(), "value-one-data"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(((ValueArgument) args.getArgument("value-three")).getValue(), ""); //$NON-NLS-1$ //$NON-NLS-2$

        args = ArgumentParser.parse(new String[]
        {
            "--value-one", "value-one-data", "--value-three", "" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }, TEST_ARGUMENTS);

        assertEquals(((ValueArgument) args.getArgument("value-one")).getValue(), "value-one-data"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(((ValueArgument) args.getArgument("value-three")).getValue(), ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testFreeArgument()
        throws Exception
    {
        ArgumentCollection args = ArgumentParser.parse(new String[]
        {
            "--choice-1", "free-value" //$NON-NLS-1$ //$NON-NLS-2$
        }, TEST_ARGUMENTS);

        assertTrue(args.contains("choice-1")); //$NON-NLS-1$
        assertEquals(((FreeArgument) args.getArgument("free-1")).getValue(), "free-value"); //$NON-NLS-1$ //$NON-NLS-2$

        args = ArgumentParser.parse(new String[]
        {
            "--choice-1", "free-value", "second-free-value" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }, TEST_ARGUMENTS);

        assertTrue(args.contains("choice-1")); //$NON-NLS-1$
        assertEquals(((FreeArgument) args.getArgument("free-1")).getValue(), "free-value"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(((FreeArgument) args.getArgument("free-2")).getValue(), "second-free-value"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testRequiredFreeArgument()
        throws Exception
    {
        Argument[] requiredFreeArgument = new Argument[]
        {
            new FreeArgument("required-one", "required one", ArgumentOptions.REQUIRED), //$NON-NLS-1$ //$NON-NLS-2$
            new FreeArgument("required-two", "required two", ArgumentOptions.REQUIRED), //$NON-NLS-1$ //$NON-NLS-2$
            new FreeArgument("not-required-three", "not required three"), //$NON-NLS-1$ //$NON-NLS-2$
        };

        ArgumentCollection args = ArgumentParser.parse(new String[]
        {
            "required-value-one", "required-value-two", "value-three" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }, requiredFreeArgument);

        assertEquals(((FreeArgument) args.getArgument("required-one")).getValue(), "required-value-one"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(((FreeArgument) args.getArgument("required-two")).getValue(), "required-value-two"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(((FreeArgument) args.getArgument("not-required-three")).getValue(), "value-three"); //$NON-NLS-1$ //$NON-NLS-2$

        args = ArgumentParser.parse(new String[]
        {
            "required-value-one", "required-value-two" //$NON-NLS-1$ //$NON-NLS-2$
        }, requiredFreeArgument);

        assertEquals(((FreeArgument) args.getArgument("required-one")).getValue(), "required-value-one"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(((FreeArgument) args.getArgument("required-two")).getValue(), "required-value-two"); //$NON-NLS-1$ //$NON-NLS-2$

        try
        {
            args = ArgumentParser.parse(new String[]
            {
                "required-value-one", "" //$NON-NLS-1$ //$NON-NLS-2$
            }, requiredFreeArgument);

            throw new AssertionFailedError("no value"); //$NON-NLS-1$
        }
        catch (ArgumentParserException e)
        {
        }
    }

    public void testTwoStageParsing()
        throws Exception
    {
        Argument[] stageOne = new Argument[]
        {
            new SwitchArgument("stage-one", "stage one"), new FreeArgument("command", "command"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        };

        ArgumentCollection stageOneArgs = ArgumentParser.parse(new String[]
        {
            "--stage-one", "test-command", "--other-argument" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }, stageOne, ArgumentParserOptions.ALLOW_UNKNOWN_ARGUMENTS);

        assertTrue(stageOneArgs.contains("stage-one")); //$NON-NLS-1$
        assertTrue(stageOneArgs.contains("command")); //$NON-NLS-1$
        assertTrue(((FreeArgument) stageOneArgs.getArgument("command")).getValue().equals("test-command")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(stageOneArgs.getUnknownArguments().length == 1);
        assertTrue(stageOneArgs.getUnknownArguments()[0].equals("--other-argument")); //$NON-NLS-1$
    }
}
