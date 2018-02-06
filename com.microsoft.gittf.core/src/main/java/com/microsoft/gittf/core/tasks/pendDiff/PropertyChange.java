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

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;

import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;

/**
 * Represents an PROPERTY change in the git repository that can be pended
 * against TFS
 * 
 */
public class PropertyChange
    extends Change
{
    final FileMode oldMode;
    final FileMode newMode;

    /**
     * Constructor
     * 
     * @param path
     *        the item path in the git repository
     * @param objectID
     *        the object id of the item
     * @param mode
     *        the file mode of the item
     */
    public PropertyChange(final String path, final ObjectId objectID, final FileMode mode)
    {
        this(path, objectID, FileMode.MISSING, mode);
    }

    /**
     * Constructor
     * 
     * @param path
     *        the item path in the git repository
     * @param objectID
     *        the object id of the item
     * @param oldMode
     *        the old file mode of the item
     * @param newMode
     *        the new file mode of the item
     */
    public PropertyChange(final String path, final ObjectId objectID, final FileMode oldMode, final FileMode newMode)
    {
        super(path, objectID);
        this.oldMode = oldMode;
        this.newMode = newMode;
    }

    public boolean isExecutablePropertyChanged()
    {
        return (oldMode == FileMode.EXECUTABLE_FILE) != (newMode == FileMode.EXECUTABLE_FILE);
    }

    public boolean isPropertyChanged()
    {
        return isExecutablePropertyChanged();
    }

    public PropertyValue getExecutablePropertyValue()
    {
        if (isExecutable())
        {
            return PropertyConstants.EXECUTABLE_ENABLED_VALUE;
        }
        else
        {
            return PropertyConstants.EXECUTABLE_DISABLED_VALUE;
        }
    }

    private boolean isExecutable()
    {
        return newMode == FileMode.EXECUTABLE_FILE;
    }
}
