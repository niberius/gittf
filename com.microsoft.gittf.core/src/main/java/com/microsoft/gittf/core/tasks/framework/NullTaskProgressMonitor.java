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

/**
 * An implementation of {@link TaskProgressMonitor} that ignores task progress.
 * 
 */
public class NullTaskProgressMonitor
    extends BaseTaskProgressMonitor
{
    private boolean inTask = false;
    private String task;
    private int work;
    private TaskProgressDisplay displayOptions = TaskProgressDisplay.NONE;
    private String detail;

    public NullTaskProgressMonitor()
    {
    }

    public void beginTask(String task, int work, final TaskProgressDisplay displayOptions)
    {
        if (this.inTask)
        {
            return;
        }

        this.inTask = true;
        this.task = task;
        this.work = work;
        this.displayOptions = displayOptions;
    }

    public String getTask()
    {
        return task;
    }

    public int getWork()
    {
        return work;
    }

    public void setWork(int work)
    {
        this.work = work;
    }

    public TaskProgressDisplay getTaskProgressDisplayOptions()
    {
        return displayOptions;
    }

    public TaskProgressMonitor newSubTask(final int subWork)
    {
        return new NullTaskProgressMonitor();
    }

    public void setDetail(final String message)
    {
        this.detail = message;
    }

    public String getDetail()
    {
        return detail;
    }

    public void worked(final double amount)
    {
    }

    public void displayMessage(final String message)
    {
    }

    public void displayWarning(final String message)
    {
    }

    public void displayVerbose(final String message)
    {
    }

    public void endTask()
    {
        if (!this.inTask)
        {
            return;
        }

        this.inTask = false;
        this.task = null;
        this.work = 0;
        this.displayOptions = TaskProgressDisplay.NONE;
        this.detail = null;
    }

    public void dispose()
    {
    }
}
