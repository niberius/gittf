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
 * Base class for command line argument identifiers.
 * 
 */
public abstract class Argument
{
    private final String name;
    private final String helpText;
    private final ArgumentOptions options;

    /**
     * Constructor base.
     * 
     * Implementers may call if they require a nonstandard implementation - for
     * example, {@link LiteralArgument}.
     */
    protected Argument()
    {
        this.name = null;
        this.helpText = null;
        this.options = ArgumentOptions.NONE;
    }

    /**
     * @equivalence Argument(name, helpText, ArgumentOptions.NONE)
     */
    public Argument(String name, String helpText)
    {
        this(name, helpText, ArgumentOptions.NONE);
    }

    /**
     * Constructor base.
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
    public Argument(String name, String helpText, ArgumentOptions options)
    {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$
        Check.notNullOrEmpty(helpText, "helpText"); //$NON-NLS-1$

        this.name = name;
        this.helpText = helpText;
        this.options = options;
    }

    /**
     * Copy constructor base.
     * 
     * Implementers should override and provide a constructor that calls this
     * base constructor and deep-copies any additional fields.
     * 
     * @param other
     *        {@link Argument} to clone (must not be <code>null</code>)
     */
    protected Argument(Argument other)
    {
        Check.notNull(other, "other"); //$NON-NLS-1$

        this.name = other.name;
        this.helpText = other.helpText;
        this.options = other.options;
    }

    /**
     * Provides the name of this argument.
     * 
     * @return The name of this argument (never <code>null</code>).
     */
    public String getName()
    {
        return name;
    }

    /**
     * Provides a short description of this argument.
     * 
     * @return The description of this argument (never <code>null</code>).
     */
    public String getHelpText()
    {
        return helpText;
    }

    /**
     * Provides the options given for this argument.
     * 
     * @return The {@link ArgumentOptions} for this argument (never
     *         <code>null</code>)
     */
    public ArgumentOptions getOptions()
    {
        return options;
    }

    /**
     * Provides a clone of this argument.
     * 
     * Implementers must override to provide a (deep copied) duplicate of the
     * current object.
     */
    @Override
    public abstract Argument clone();

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        int hashCode = 31 + (name != null ? name.hashCode() : 0);
        hashCode = 31 * hashCode + (helpText != null ? helpText.hashCode() : 0);
        hashCode = 31 * hashCode + options.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof Argument))
        {
            return false;
        }

        Argument other = (Argument) o;

        if ((name == null && other.name != null) || (name != null && !name.equals(other.name)))
        {
            return false;
        }

        if ((helpText == null && other.helpText != null) || (helpText != null && !helpText.equals(other.helpText)))
        {
            return false;
        }

        if (!options.equals(other.options))
        {
            return false;
        }

        return true;
    }
}