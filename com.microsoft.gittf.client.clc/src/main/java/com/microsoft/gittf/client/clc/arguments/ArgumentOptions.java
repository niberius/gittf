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

package com.microsoft.gittf.client.clc.arguments;

import com.microsoft.tfs.util.BitField;

/**
 * Argument options control the parsing of arguments and the display of help for
 * arguments.
 * 
 */
public class ArgumentOptions
    extends BitField
{
    private static final long serialVersionUID = -8592319567263147472L;

    /**
     * No options
     */
    public static final ArgumentOptions NONE = new ArgumentOptions(0);

    /**
     * This argument should not be displayed in help.
     */
    public static final ArgumentOptions HIDDEN = new ArgumentOptions(1);

    /**
     * This argument is required.
     */
    public static final ArgumentOptions REQUIRED = new ArgumentOptions(2);

    /**
     * The value for this argument is required.
     */
    public static final ArgumentOptions VALUE_REQUIRED = new ArgumentOptions(4);

    /**
     * This argument may contain literal input - used for FreeArgumentCollection
     * to consume all remaining arguments.
     */
    public static final ArgumentOptions LITERAL = new ArgumentOptions(8);

    /**
     * Multiple arguments may be specified.
     */
    public static final ArgumentOptions MULTIPLE = new ArgumentOptions(16);

    /**
     * Argument may be empty. The default for {@link FreeArgument}s that are
     * {@link ArgumentOptions#REQUIRED} and {@link ValueArgument}s that are
     * {@link ArgumentOptions#VALUE_REQUIRED} is that they also not be empty.
     */
    public static final ArgumentOptions EMPTY = new ArgumentOptions(32);

    /**
     * The presence of this argument informs the parser that other requirements
     * should be suppressed. For example, --help will typically turn off
     * required argument enforcement.
     */
    public static final ArgumentOptions SUPPRESS_REQUIREMENTS = new ArgumentOptions(64);

    /**
     * Constructor
     * 
     * @param value
     */
    private ArgumentOptions(int value)
    {
        super(value);
    }

    public static ArgumentOptions combine(ArgumentOptions... options)
    {
        return new ArgumentOptions(BitField.combine(options));
    }

    public ArgumentOptions combine(ArgumentOptions other)
    {
        return new ArgumentOptions(super.combineInternal(other));
    }

    public boolean contains(ArgumentOptions other)
    {
        return super.containsInternal(other);
    }
}
