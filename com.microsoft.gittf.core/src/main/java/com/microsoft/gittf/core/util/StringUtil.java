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

package com.microsoft.gittf.core.util;

public final class StringUtil
{
    /**
     * Constructor
     */
    private StringUtil()
    {

    }

    /**
     * Converts an object array to a string array
     * 
     * @param objectArray
     * @return
     */
    public static String[] convertToStringArray(Object[] objectArray)
    {
        Check.notNullOrEmpty(objectArray, "objectArray"); //$NON-NLS-1$

        String[] toReturn = new String[objectArray.length];
        for (int count = 0; count < objectArray.length; count++)
        {
            toReturn[count] = objectArray[count].toString();
        }

        return toReturn;
    }

    public static boolean isNullOrEmpty(final String s)
    {
        return s == null || s.length() == 0;
    }
}
