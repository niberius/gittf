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

package com.microsoft.gittf.core.tasks.pendDiff;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;

/**
 * Utilities to work with the CheckinAnalysisChangeCollection class *
 * 
 */
public final class CheckinAnalysisChangeCollectionUtil
{
    private CheckinAnalysisChangeCollectionUtil()
    {

    }

    /**
     * Determines if the collection contains the path specified
     * 
     * @param analysis
     *        the analysis object
     * @param path
     *        the path to lookup
     * @return
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws CorruptObjectException
     * @throws IOException
     */
    public static boolean contains(CheckinAnalysisChangeCollection analysis, String path)
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        if (contains(analysis.getAdds(), path))
        {
            return true;
        }

        if (contains(analysis.getDeletes(), path))
        {
            return true;
        }

        if (contains(analysis.getEdits(), path))
        {
            return true;
        }

        if (contains(analysis.getRenames(), path))
        {
            return true;
        }

        if (contains(analysis.getProperties(), path))
        {
            return true;
        }

        return false;
    }

    public static boolean contains(List<? extends Change> changes, String path)
    {
        for (Change change : changes)
        {
            if (change.getPath().equals(path))
            {
                return true;
            }
        }

        return false;
    }

    public static Change getChange(List<? extends Change> changes, String path)
    {
        for (Change change : changes)
        {
            if (change.getPath().equals(path))
            {
                return change;
            }
        }

        return null;
    }
}
