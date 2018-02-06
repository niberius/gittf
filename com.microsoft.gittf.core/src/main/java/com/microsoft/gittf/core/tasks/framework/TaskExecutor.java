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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.gittf.core.util.Check;

/**
 * The task executor class that is responsible for executing any Task
 * 
 */
public class TaskExecutor
{
    private static final Log log = LogFactory.getLog(TaskExecutor.class);

    private final TaskProgressMonitor progressMonitor;

    private final List<TaskStartedHandler> taskStartedHandlers = new ArrayList<TaskStartedHandler>();
    private final List<TaskCompletedHandler> taskCompletedHandlers = new ArrayList<TaskCompletedHandler>();

    /**
     * Constructor
     * 
     * @param progressMonitor
     *        the progress monitor to use to report progress
     */
    public TaskExecutor(final TaskProgressMonitor progressMonitor)
    {
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        this.progressMonitor = progressMonitor;
    }

    /**
     * Adds a TaskStartedHandler
     * 
     * @param handler
     * @return
     */
    public final boolean addTaskStartedHandler(TaskStartedHandler handler)
    {
        Check.notNull(handler, "handler"); //$NON-NLS-1$

        return taskStartedHandlers.add(handler);
    }

    /**
     * Removes a TaskStartedHandler
     * 
     * @param handler
     * @return
     */
    public final boolean removeTaskStartedHandler(TaskStartedHandler handler)
    {
        Check.notNull(handler, "handler"); //$NON-NLS-1$

        return taskStartedHandlers.remove(handler);
    }

    /**
     * Adds a TaskCompletedHandler
     * 
     * @param handler
     * @return
     */
    public final boolean addTaskCompletedHandler(TaskCompletedHandler handler)
    {
        Check.notNull(handler, "handler"); //$NON-NLS-1$

        return taskCompletedHandlers.add(handler);
    }

    /**
     * Removes a TaskCompletedHandler
     * 
     * @param handler
     * @return
     */
    public final boolean removeTaskCompletedHandler(TaskCompletedHandler handler)
    {
        Check.notNull(handler, "handler"); //$NON-NLS-1$

        return taskCompletedHandlers.remove(handler);
    }

    /**
     * Executes the specified task
     * 
     * @param task
     *        to execute
     * @return
     */
    public TaskStatus execute(final Task task)
    {
        Check.notNull(task, "task"); //$NON-NLS-1$

        TaskStatus status;

        /* Calls the task started handlers */
        for (TaskStartedHandler handler : taskStartedHandlers)
        {
            try
            {
                handler.onTaskStarted(task);
            }
            catch (Exception e)
            {
                log.warn(MessageFormat.format("Exception while notifying task start handler {0} for task {1}", //$NON-NLS-1$
                    handler.getClass().getSimpleName(),
                    task.getClass().getSimpleName()), e);
            }
        }

        /* Runs the task */
        try
        {
            status = task.run(progressMonitor);
        }
        catch (Exception e)
        {
            status = new TaskStatus(TaskStatus.ERROR, e);
        }
        finally
        {
            progressMonitor.dispose();
        }

        /* Calls the task completed handlers */
        for (TaskCompletedHandler handler : taskCompletedHandlers)
        {
            try
            {
                handler.onTaskCompleted(task, status);
            }
            catch (Exception e)
            {
                log.warn(MessageFormat.format("Exception while notifying task completed handler {0} for task {1}", //$NON-NLS-1$
                    handler.getClass().getSimpleName(),
                    task.getClass().getSimpleName()), e);
            }
        }

        return status;
    }
}
