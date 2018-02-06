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

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;

import com.microsoft.gittf.core.util.Check;

/**
 * Represents an entry in the commit tree for a file or a folder
 * 
 */
public class CommitTreeEntry
{
    private final FileMode mode;
    private final ObjectId objectID;

    /**
     * Constructor
     * 
     * @param mode
     *        the file mode of the object
     * @param objectID
     *        the object id
     */
    public CommitTreeEntry(FileMode mode, ObjectId objectID)
    {
        Check.notNull(mode, "mode"); //$NON-NLS-1$
        Check.notNull(objectID, "objectID"); //$NON-NLS-1$

        this.mode = mode;
        this.objectID = objectID;
    }

    /**
     * Get the file mode
     * 
     * @return FileMode for the entry
     */
    public FileMode getFileMode()
    {
        return mode;
    }

    /**
     * Get the object id
     * 
     * @return ObjectId for the entry
     */
    public ObjectId getObjectID()
    {
        return objectID;
    }
}