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

package com.microsoft.gittf.core.tasks.framework;

import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.util.BitField;

/**
 * Task options
 * 
 */
public class TaskOptions
    extends BitField
{
    private static final long serialVersionUID = 1206939419418542765L;

    public static final TaskOptions NONE = new TaskOptions(0);

    /**
     * Constructor
     * 
     * @param value
     */
    private TaskOptions(int value)
    {
        super(value);
    }

    /**
     * Tests whether the otions contain the specified option or not
     * 
     * @param other
     * @return
     */
    public boolean contains(TaskOptions other)
    {
        Check.notNull(other, "other"); //$NON-NLS-1$

        return super.containsInternal(other);
    }

    /**
     * Combines multiple task options together
     * 
     * @param other
     * @return
     */
    public TaskOptions combine(TaskOptions other)
    {
        Check.notNull(other, "other"); //$NON-NLS-1$

        return new TaskOptions(super.combineInternal(other));
    }
}