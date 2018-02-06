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

import java.util.ArrayList;
import java.util.List;

import com.microsoft.gittf.core.util.Check;

/**
 * An {@link Argument} type that collects a number of free arguments on the
 * command line. For example, in the command line "cat one two", the file names
 * "one" and "two" are a collection of free arguments.
 * 
 */
public class FreeArgumentCollection
    extends Argument
{
    private final List<String> values = new ArrayList<String>();

    /**
     * @equivalence FreeArgumentCollection(name, helpText, ArgumentOptions.NONE)
     */
    public FreeArgumentCollection(String name, String helpText)
    {
        this(name, helpText, ArgumentOptions.NONE);
    }

    /**
     * Creates a collection of free arguments at the current position.
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
    public FreeArgumentCollection(String name, String helpText, ArgumentOptions options)
    {
        super(name, helpText, options);
    }

    private FreeArgumentCollection(FreeArgumentCollection other)
    {
        super(other);

        Check.notNull(other, "other"); //$NON-NLS-1$

        for (String value : other.values)
        {
            values.add(value);
        }
    }

    /**
     * Adds a value for this argument
     * 
     * @param value
     *        The value given to this argument. May not be <code>null</code>.
     */
    public void addValue(String value)
    {
        Check.notNull(value, "value"); //$NON-NLS-1$

        values.add(value);
    }

    /**
     * Gets the values that were provided to this argument.
     * 
     * @param value
     *        An array of values provided to this argument (never
     *        <code>null</code>)
     */
    public String[] getValues()
    {
        return values.toArray(new String[values.size()]);
    }

    @Override
    public Argument clone()
    {
        return new FreeArgumentCollection(this);
    }

    @Override
    public int hashCode()
    {
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + values.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof FreeArgumentCollection))
        {
            return false;
        }

        FreeArgumentCollection other = (FreeArgumentCollection) o;

        if (!values.equals(other.values))
        {
            return false;
        }

        return true;
    }
}
