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

import com.microsoft.gittf.core.util.Check;

/**
 * An {@link Argument} type that is specified by name on the command line - for
 * example "--foo" or "-f". These options do not take a value and are simply
 * either specified or not.
 * 
 */
public class SwitchArgument
    extends NamedArgument
{
    /**
     * @equivalence SwitchArgument(name, (char) 0, helpText,
     *              ArgumentOptions.NONE)
     */
    public SwitchArgument(String name, String helpText)
    {
        this(name, (char) 0, helpText, ArgumentOptions.NONE);
    }

    /**
     * @equivalence SwitchArgument(name, (char) 0, helpText, options)
     */
    public SwitchArgument(String name, String helpText, ArgumentOptions options)
    {
        this(name, (char) 0, helpText, options);
    }

    /**
     * @equivalence SwitchArgument(name, alias, helpText, ArgumentOptions.NONE)
     */
    public SwitchArgument(String name, char alias, String helpText)
    {
        this(name, alias, helpText, ArgumentOptions.NONE);
    }

    /**
     * Creates a new argument that may be specified by name on the command line
     * which does not accept a value.
     * 
     * @param name
     *        The long name of the argument, as accepted on the command-line
     *        prefixed by two dashes ('--'). May not be <code>null</code>.
     * @param alias
     *        The short name of the argument, as accepted on the command line
     *        prefixed by a single dash ('-'). May be <code>(char) 0</code> to
     *        indicate that there is no short name alias.
     * @param helpText
     *        The description of this argument, provided as help to the user.
     *        May not be <code>null</code>.
     * @param options
     *        Options for this argument. May not be <code>null</code>.
     */
    public SwitchArgument(String name, char alias, String helpText, ArgumentOptions options)
    {
        super(name, alias, helpText, options);
    }

    private SwitchArgument(SwitchArgument other)
    {
        super(other);

        Check.notNull(other, "other"); //$NON-NLS-1$
    }

    @Override
    public Argument clone()
    {
        return new SwitchArgument(this);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof SwitchArgument))
        {
            return false;
        }

        if (!super.equals(o))
        {
            return false;
        }

        return (o instanceof SwitchArgument);
    }
}