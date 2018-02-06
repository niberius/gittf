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

import java.text.MessageFormat;

import com.microsoft.gittf.core.util.Check;

/**
 * Base class for arguments that are specified by name on the command line.
 * Arguments may be specified in long-form, prefixed by two dashes ('--'), for
 * example "--foo", or may optionally be specified in short form with a
 * single-character alias prefixed by a single dash ('-'), for example '-f'.
 */
public abstract class NamedArgument
    extends Argument
{
    private final char alias;

    /**
     * Creates a new argument that may be specified by name on the command line.
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
    public NamedArgument(String name, char alias, String helpText, ArgumentOptions options)
    {
        super(name, helpText, options);

        this.alias = alias;
    }

    protected NamedArgument(NamedArgument other)
    {
        super(other);

        Check.notNull(other, "other"); //$NON-NLS-1$

        this.alias = other.alias;
    }

    /**
     * Gets the single character alias for this argument, or <code>0</code> if
     * none is specified.
     * 
     * @return The single character alias for this argument or <code>0</code>.
     */
    public char getAlias()
    {
        return alias;
    }

    @Override
    public String toString()
    {
        if (alias == 0)
        {
            return MessageFormat.format("--{0}", getName()); //$NON-NLS-1$
        }
        else
        {
            return MessageFormat.format("--{0}/-{1}", getName(), alias); //$NON-NLS-1$
        }
    }

    @Override
    public int hashCode()
    {
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + alias;
        return hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof NamedArgument))
        {
            return false;
        }

        if (!super.equals(o))
        {
            return false;
        }

        NamedArgument other = (NamedArgument) o;

        if (alias != other.alias)
        {
            return false;
        }

        return true;
    }
}
