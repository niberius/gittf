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

/**
 * Indicates the "literal argument", specified as two dashes ("--"), which
 * indicates that all future arguments should be treated literally (as free
 * arguments) and not as named arguments.
 * 
 * This allows a command-line argument that would have been treated as a switch
 * argument to be treated as a free argument instead. For example, to specify a
 * file named "--help", the command line may be:
 * 
 * <code>rm -- --help</code>
 * 
 */
public final class LiteralArgument
    extends Argument
{
    /**
     * Constructs a literal argument separator.
     */
    public LiteralArgument()
    {
    }

    @Override
    public Argument clone()
    {
        throw new RuntimeException("cannot clone literalargument"); //$NON-NLS-1$
    }

    @Override
    public int hashCode()
    {
        /*
         * All LiteralArguments are equal, so this is just some random int -
         * don't call super() because that would just use 31, which seems like a
         * likely hash bucket collision vector.
         */
        return 219540062;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof LiteralArgument)
        {
            return true;
        }

        return false;
    }
}
