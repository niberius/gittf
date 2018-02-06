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

package com.microsoft.gittf.core.util.tree;

import java.util.Comparator;

/**
 * Compares two CommitTreePath objects to sort them correctly when building a
 * commit tree
 * 
 */
public class CommitTreePathComparator
    implements Comparator<CommitTreePath>
{
    /**
     * Compares two CommitTreePath objects
     */
    public int compare(CommitTreePath x, CommitTreePath y)
    {
        int xDepth = x.getDepth();
        int yDepth = y.getDepth();

        // compare the items only if the depth is the same
        if (xDepth == yDepth)
        {
            return x.getFullName().toLowerCase().compareTo(y.getFullName().toLowerCase());
        }

        return yDepth - xDepth;
    }
}
