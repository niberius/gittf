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

import com.microsoft.tfs.util.BitField;

/**
 * Task display progress options
 * 
 */
public final class TaskProgressDisplay
    extends BitField
{
    private static final long serialVersionUID = -179375741555801765L;

    /**
     * Display no progress
     */
    public static final TaskProgressDisplay NONE = new TaskProgressDisplay(0);

    /**
     * Display task progress only
     */
    public static final TaskProgressDisplay DISPLAY_PROGRESS = new TaskProgressDisplay(1);

    /**
     * Display task and sub task progress
     */
    public static final TaskProgressDisplay DISPLAY_SUBTASK_DETAIL = new TaskProgressDisplay(2);

    /**
     * Default is to display task and sub task progress
     */
    public static final TaskProgressDisplay DEFAULT = DISPLAY_SUBTASK_DETAIL;

    /**
     * Constructor
     * 
     * @param flags
     */
    private TaskProgressDisplay(int flags)
    {
        super(flags);
    }

    /**
     * Tests whether the option is contained or not
     * 
     * @param other
     * @return
     */
    public boolean contains(TaskProgressDisplay other)
    {
        return super.containsInternal(other);
    }

    /**
     * Combines multiple options
     * 
     * @param other
     * @return
     */
    public TaskProgressDisplay combine(final TaskProgressDisplay other)
    {
        return new TaskProgressDisplay(super.combineInternal(other));
    }
}