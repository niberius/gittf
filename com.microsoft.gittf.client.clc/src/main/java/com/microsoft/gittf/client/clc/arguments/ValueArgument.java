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
 * An {@link Argument} type that takes an value - for example "--one=bar",
 * "--one bar" or "-o bar". Values may be optionally required (
 * {@link ArgumentOptions#VALUE_REQUIRED}).
 * 
 */
public class ValueArgument
    extends NamedArgument
{
    private final String valueDescription;

    private String value;

    /**
     * @equivalence ValueArgument(name, (char) 0, valueDescription, helpText,
     *              ArgumentOptions.NONE)
     */
    public ValueArgument(String name, String valueDescription, String helpText)
    {
        this(name, (char) 0, valueDescription, helpText, ArgumentOptions.NONE);
    }

    /**
     * @equivalence ValueArgument(name, (char) 0, valueDescription, helpText,
     *              options)
     */
    public ValueArgument(String name, String valueDescription, String helpText, ArgumentOptions options)
    {
        this(name, (char) 0, valueDescription, helpText, options);
    }

    /**
     * @equivalence ValueArgument(name, alias, valueDescription, helpText,
     *              ArgumentOptions.NONE)
     */
    public ValueArgument(String name, char alias, String valueDescription, String helpText)
    {
        this(name, alias, valueDescription, helpText, ArgumentOptions.NONE);
    }

    /**
     * Creates a new argument that (optionally) takes a value.
     * 
     * @param name
     *        The long name of the argument, as accepted on the command-line
     *        prefixed by two dashes ('--'). May not be <code>null</code>.
     * @param alias
     *        The short name of the argument, as accepted on the command line
     *        prefixed by a single dash ('-'). May be <code>(char) 0</code> to
     *        indicate that there is no short name alias.
     * @param valueDescription
     *        The description of the value, provided as help to the user. May
     *        not be <code>null</code>.
     * @param helpText
     *        The description of this argument, provided as help to the user.
     *        May not be <code>null</code>.
     * @param options
     *        Options for this argument. May not be <code>null</code>.
     */
    public ValueArgument(String name, char alias, String valueDescription, String helpText, ArgumentOptions options)
    {
        super(name, alias, helpText, options);

        Check.notNullOrEmpty(valueDescription, "valueDescription"); //$NON-NLS-1$

        this.valueDescription = valueDescription;
    }

    private ValueArgument(ValueArgument other)
    {
        super(other);

        Check.notNull(other, "other"); //$NON-NLS-1$

        this.valueDescription = other.valueDescription;
        this.value = other.value;
    }

    /**
     * Gets the help text description of the value that is provided to the
     * argument.
     * 
     * @return The help text description (never <code>null</code>).
     */
    public String getValueDescription()
    {
        return valueDescription;
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
        return new ValueArgument(this);
    }

    @Override
    public int hashCode()
    {
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + valueDescription.hashCode();
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

        if (!(o instanceof ValueArgument))
        {
            return false;
        }

        if (!super.equals(o))
        {
            return false;
        }

        ValueArgument other = (ValueArgument) o;

        if (!valueDescription.equals(other.valueDescription.hashCode()))
        {
            return false;
        }

        if ((value == null && other.value != null) || (value != null && !value.equals(other)))
        {
            return false;
        }

        return true;
    }
}
