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
 * An {@link Argument} type that is specified on the command line and not
 * prefixed with a name. Free arguments are generally identified by position.
 * For example, in the command line "cat foo", then file name "foo" would be
 * specified as a "free argument".
 * 
 */
public class FreeArgument
    extends Argument
{
    private String value;

    /**
     * @equivalence FreeArgument(name, helpText, ArgumentOptions.NONE)
     */
    public FreeArgument(String name, String helpText)
    {
        this(name, helpText, ArgumentOptions.NONE);
    }

    /**
     * Creates a free argument at the current position.
     * 
     * @param name
     *        The name of the argument, for use in retrieving the argument. May
     *        not be <code>null</code>.
     * @param helpText
     *        The description of this argument, provided as help to the user.
     *        May not be <code>null</code>.
     * @param options
     *        Options for this argument. May not be <code>null</code>.
     */
    public FreeArgument(String name, String helpText, ArgumentOptions options)
    {
        super(name, helpText, options);
    }

    private FreeArgument(FreeArgument other)
    {
        super(other);

        Check.notNull(other, "other"); //$NON-NLS-1$

        this.value = other.value;
    }

    /**
     * Sets the value that was provided to this argument.
     * 
     * @param value
     *        The value provided to this argument or <code>null</code> if it was
     *        not provided.
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * Gets the value that was provided to this argument.
     * 
     * @param value
     *        The value provided to this argument or <code>null</code> if it was
     *        not provided.
     */
    public String getValue()
    {
        return value;
    }

    @Override
    public Argument clone()
    {
        return new FreeArgument(this);
    }

    @Override
    public int hashCode()
    {
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + ((value == null) ? 0 : value.hashCode());
        return hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof FreeArgument))
        {
            return false;
        }

        if (!super.equals(o))
        {
            return false;
        }

        FreeArgument other = (FreeArgument) o;

        if ((value == null && other.value != null) || (value != null && !value.equals(other)))
        {
            return false;
        }

        return true;
    }
}
