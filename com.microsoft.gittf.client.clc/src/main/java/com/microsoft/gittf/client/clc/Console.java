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

import java.io.PrintStream;

import com.microsoft.gittf.core.util.NullPrintStream;
import com.microsoft.tfs.jni.ConsoleUtils;
import com.microsoft.tfs.util.Check;

/**
 * Defines a console implementation that will be used by git-tf
 * 
 */
public class Console
{
    /**
     * The verbosity that the console adheres to
     * 
     */
    public enum Verbosity
    {
        QUIET(-1), NORMAL(0), VERBOSE(1);

        private final int value;

        private Verbosity(int value)
        {
            this.value = value;
        }

        public final int getValue()
        {
            return value;
        }
    };

    private static final int CONSOLE_MIN_WIDTH_FOR_OVERWRITE = 8;
    private static final PrintStream outputStream = System.out;
    private static final PrintStream errorStream = System.err;

    private Verbosity verbosity = Verbosity.NORMAL;

    /**
     * Constructor
     */
    public Console()
    {
    }

    public void setVerbosity(Verbosity verbosity)
    {
        Check.notNull(verbosity, "verbosity"); //$NON-NLS-1$

        this.verbosity = verbosity;
    }

    public Verbosity getVerbosity()
    {
        return verbosity;
    }

    public PrintStream getOutputStream()
    {
        return outputStream;
    }

    public PrintStream getErrorStream()
    {
        return errorStream;
    }

    public PrintStream getOutputStream(Verbosity verbosity)
    {
        if (verbosity.getValue() <= this.verbosity.getValue())
        {
            return outputStream;
        }

        return new NullPrintStream();
    }

    public int getWidth()
    {
        return ConsoleUtils.getInstance().getConsoleColumns();
    }

    public boolean supportsOverwrite()
    {
        /*
         * If we can't determine the console width, then that likely means we're
         * running in a console emulator that is incapable of displaying
         * backspaces properly. The Eclipse Console is an example of this. In
         * this case we should suggest clients do not overwrite lines by using
         * backspace.
         */
        return (ConsoleUtils.getInstance().getConsoleColumns() >= CONSOLE_MIN_WIDTH_FOR_OVERWRITE);
    }
}
