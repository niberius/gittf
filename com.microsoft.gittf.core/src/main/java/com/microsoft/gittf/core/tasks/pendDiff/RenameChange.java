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

import org.eclipse.jgit.lib.ObjectId;

import com.microsoft.gittf.core.util.Check;

/**
 * Represents a RENAME change in the git repository that can be pended against
 * TFS
 * 
 */
public class RenameChange
    extends Change
{
    private final String oldPath;
    private boolean isEdit;

    /**
     * Constructor
     * 
     * @param oldPath
     *        the old path of the item
     * @param newPath
     *        the new path of the item
     * @param objectID
     *        the object id of the item in the git repository
     * @param isEdit
     *        is this a pure rename or is a rename edit
     */
    public RenameChange(final String oldPath, final String newPath, final ObjectId objectID, boolean isEdit)
    {
        super(newPath, objectID);

        Check.notNullOrEmpty(oldPath, "oldPath"); //$NON-NLS-1$
        Check.notNullOrEmpty(newPath, "newPath"); //$NON-NLS-1$

        this.oldPath = oldPath;
        this.isEdit = isEdit;
    }

    /**
     * Gets the old path of the item
     * 
     * @return
     */
    public String getOldPath()
    {
        return oldPath;
    }

    /**
     * Gets the new path of the item
     * 
     * @return
     */
    public String getNewPath()
    {
        return getPath();
    }

    /**
     * Returns true if this change is a rename, edit
     * 
     * @return
     */
    public boolean isEdit()
    {
        return isEdit;
    }

    /**
     * Updates the edit information if needed
     * 
     * @param objectID
     *        the new object Id
     */
    public void updateEditInformation(ObjectId objectID)
    {
        isEdit = !ObjectId.zeroId().equals(objectID);
        this.objectID = objectID;
    }
}
