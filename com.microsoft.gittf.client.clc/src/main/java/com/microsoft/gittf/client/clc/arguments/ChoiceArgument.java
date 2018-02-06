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

import java.util.Arrays;

import com.microsoft.gittf.core.util.Check;

/**
 * Defines a choice argument
 * 
 */
public class ChoiceArgument
    extends Argument
{
    private final String helpText;
    private final Argument[] arguments;

    /**
     * Constructor
     * 
     * @param arguments
     */
    public ChoiceArgument(Argument... arguments)
    {
        Check.notNull(arguments, "argumnets"); //$NON-NLS-1$

        this.helpText = null;
        this.arguments = arguments;
    }

    /**
     * Constructor
     * 
     * @param helpText
     * @param arguments
     */
    public ChoiceArgument(String helpText, Argument... arguments)
    {
        Check.notNullOrEmpty(helpText, "helpText"); //$NON-NLS-1$
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$

        this.helpText = helpText;
        this.arguments = arguments;
    }

    /**
     * Constructor
     * 
     * @param other
     */
    protected ChoiceArgument(ChoiceArgument other)
    {
        Check.notNull(other, "other"); //$NON-NLS-1$

        this.helpText = other.helpText;
        this.arguments = new Argument[other.arguments.length];

        for (int i = 0; i < arguments.length; i++)
        {
            this.arguments[i] = other.arguments[i].clone();
        }
    }

    @Override
    public String getHelpText()
    {
        return helpText;
    }

    public Argument[] getArguments()
    {
        return arguments;
    }

    @Override
    public Argument clone()
    {
        return new ChoiceArgument(this);
    }

    @Override
    public int hashCode()
    {
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + helpText.hashCode();
        hashCode = 31 * hashCode + Arrays.hashCode(arguments);
        return hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof ChoiceArgument))
        {
            return false;
        }

        if (!super.equals(o))
        {
            return false;
        }

        ChoiceArgument other = (ChoiceArgument) o;

        if (!helpText.equals(other.helpText))
        {
            return false;
        }

        if (!Arrays.equals(arguments, other.arguments))
        {
            return false;
        }

        return true;
    }
}
