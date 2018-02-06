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

import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.util.Check;

/**
 * A progress monitor for sub tasks
 * 
 */
public class SubTaskProgressMonitor
    extends BaseTaskProgressMonitor
{
    private final TaskProgressMonitor parent;
    private final int parentWork;
    private final String parentDetail;

    private String task;
    private boolean inTask = false;
    private double worked = 0;
    private int workTotal = 0;

    private String detail;
    private TaskProgressDisplay displayOptions = TaskProgressDisplay.NONE;

    /**
     * Constructor
     * 
     * @param parent
     * @param parentWork
     */
    public SubTaskProgressMonitor(final TaskProgressMonitor parent, final int parentWork)
    {
        Check.notNull(parent, "parent"); //$NON-NLS-1$
        Check.isTrue(parentWork >= 0, "parentWork >= 0"); //$NON-NLS-1$

        this.parent = parent;
        this.parentWork = parentWork;
        this.parentDetail = parent.getDetail();
    }

    public void beginTask(String task, int work, TaskProgressDisplay displayOptions)
    {
        Check.notNull(task, "task"); //$NON-NLS-1$
        Check.isTrue(work >= TaskProgressMonitor.INDETERMINATE, "work >= INDETERMINATE"); //$NON-NLS-1$

        if (this.inTask)
        {
            return;
        }

        this.task = task;
        this.inTask = true;
        this.workTotal = work;
        this.displayOptions = displayOptions;
    }

    public String getTask()
    {
        return task;
    }

    public int getWork()
    {
        return workTotal;
    }

    public void setWork(int workTotal)
    {
        this.workTotal = workTotal;
        worked(0);
    }

    public TaskProgressDisplay getTaskProgressDisplayOptions()
    {
        return displayOptions;
    }

    public TaskProgressMonitor newSubTask(int subWork)
    {
        return new SubTaskProgressMonitor(this, subWork);
    }

    public void setDetail(String detail)
    {
        this.detail = detail;

        if (parent.getTaskProgressDisplayOptions().contains(TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL))
        {
            if (parentDetail != null && parentDetail.length() > 0 && detail != null && detail.length() > 0)
            {
                parent.setDetail(Messages.formatString("SubTaskProgressMonitor.DetailFormat", parentDetail, detail)); //$NON-NLS-1$
            }
            else if (parentDetail != null && parentDetail.length() > 0)
            {
                parent.setDetail(parentDetail);
            }
            else
            {
                parent.setDetail(detail);
            }
        }
    }

    public String getDetail()
    {
        return detail;
    }

    public void worked(double amount)
    {
        if (worked + amount > workTotal)
        {
            amount = workTotal - worked;
            worked = workTotal;
        }
        else
        {
            this.worked += amount;
        }

        parent.worked(((amount / workTotal) * (double) parentWork));
    }

    public void displayMessage(String message)
    {
        parent.displayMessage(message);
    }

    public void displayWarning(String message)
    {
        parent.displayWarning(message);
    }

    public void displayVerbose(String message)
    {
        parent.displayVerbose(message);
    }

    public void endTask()
    {
        if (!this.inTask)
        {
            return;
        }

        /* This task is done, notify the parent of remaining work */
        if (this.worked < this.workTotal)
        {
            this.worked(this.workTotal - this.worked);
        }

        parent.setDetail(parentDetail);

        this.inTask = false;
        this.task = null;
        this.workTotal = 0;
        this.worked = 0;
        this.displayOptions = TaskProgressDisplay.NONE;
    }

    public void dispose()
    {
    }
}
