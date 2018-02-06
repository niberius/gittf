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
 * An interface for {@link Task}s to report progress to callers. Implementations
 * should display this progress to the user in a useful way such as displaying
 * messages that the task reports and a percentage of the completion.
 * 
 * Example showing task reporting a percent of completion:
 * 
 * <code>
 * TaskProgressMonitor progressMonitor = new TaskProgressMonitorImpl();
 * 
 * try
 * {
 *     progressMonitor.beginTask("Doing some work", 100, TaskProgressDisplay.DISPLAY_PROGRESS);
 *     doSomeWork();
 *     progressMonitor.worked(50);
 *     doMoreWork();
 *     progressMonitor.worked(50);
 *     progressMonitor.endTask();
 * }
 * finally
 * {
 *     progressMonitor.dispose();
 * }
 * </code>
 * 
 * Callers should not call
 * {@link TaskProgressMonitor#beginTask(String, int, TaskProgressDisplay)}
 * multiple times on a single {@link TaskProgressMonitor}. To perform multiple
 * tasks, it may instead be useful to create another "sub task" monitor to
 * provide to another task using the {@link TaskProgressMonitor#newSubTask(int)}
 * . Sub tasks will work a given amount of the parent, reporting their work as
 * it is performed, and may optionally provide detailed messages to the parent.
 * 
 * Callers must dispose {@link TaskProgressMonitor}s they instantiate with
 * {@link TaskProgressMonitor#dispose()}. It is not necessary to dispose sub
 * task monitors created with {@link TaskProgressMonitor#newSubTask(int)}.
 * 
 */
public interface TaskProgressMonitor
{
    /**
     * Indicates that the amount of work to perform is unknown. Status display
     * should be an "indeterminate" display rather than a percentage of
     * completion.
     */
    public static final int INDETERMINATE = 0;

    /**
     * Begins a new task with the default display options for the
     * implementation.
     * 
     * @param task
     *        The name of the task to begin (never <code>null</code>)
     * @param work
     *        The amount of work that the task will perform, or
     *        {@link TaskProgressMonitor#INDETERMINATE} if it is an unknown
     *        amount of work
     */
    void beginTask(final String task, final int work);

    /**
     * Begins a new task with the default display options for the
     * implementation.
     * 
     * @param task
     *        The name of the task to begin (never <code>null</code>)
     * @param work
     *        The amount of work that the task will perform, or
     *        {@link TaskProgressMonitor#INDETERMINATE} if it is an unknown
     *        amount of work
     * @param displayOptions
     *        Hints as to how this task should be displayed.
     */
    void beginTask(final String task, final int work, final TaskProgressDisplay displayOptions);

    /**
     * Gets the name of the currently running task.
     * 
     * @return The currently running task or <code>null</code> if none is
     *         running.
     */
    String getTask();

    /**
     * The amount of work the current task performs.
     * 
     * @return The amount of work the current task performs.
     */
    int getWork();

    /**
     * Sets the amount of work the current task performs.
     * 
     * @param The
     *        amount of work the current task performs.
     */
    void setWork(int work);

    /**
     * The display options for the current task.
     * 
     * @return The {@link TaskProgressDisplay} options for the current task.
     */
    TaskProgressDisplay getTaskProgressDisplayOptions();

    /**
     * Creates a new sub task that will work the given amount of the current
     * task. Calls to the sub task's {@link TaskProgressMonitor#worked(double)}
     * method will be proxied up to this task's work.
     * 
     * @param subWork
     *        The amount of work of this task that the sub task will perform.
     * @return A new {@link TaskProgressMonitor} to perform work (never
     *         <code>null</code>)
     */
    TaskProgressMonitor newSubTask(final int subWork);

    /**
     * Sets an informative message on the status of the current task, useful for
     * reporting the current sub task information.
     * 
     * @param detail
     *        The informative message or <code>null</code> if there is none.
     */
    void setDetail(final String detail);

    /**
     * Gets the current detail message.
     * 
     * @param detail
     *        The informative message or <code>null</code> if there is none.
     */
    String getDetail();

    /**
     * Indicates that the task has worked an amount since the last call to
     * {@link TaskProgressMonitor#worked(double)}.
     * 
     * @param amount
     *        The amount worked since the last call
     */
    void worked(final int amount);

    /**
     * Indicates that the task has worked an amount since the last call to
     * {@link TaskProgressMonitor#worked(double)}.
     * 
     * @param amount
     *        The amount worked since the last call
     */
    void worked(final double amount);

    /**
     * Displays a message to the progress monitor (if the progress monitor is so
     * configured to display messages.)
     * 
     * @param message
     *        A message to display (never <code>null</code>)
     */
    void displayMessage(final String message);

    /**
     * Displays a warning message to the progress monitor. Implementations
     * should always display warning messages in some manner.
     * 
     * @param message
     *        A warning message to display (never <code>null</code>)
     */
    void displayWarning(final String message);

    /**
     * Displays a verbose message to the progress monitor (if the progress
     * monitor is so configured to display verbose messages.)
     * 
     * @param message
     *        A verbose message to display (never <code>null</code>)
     */
    void displayVerbose(final String message);

    /**
     * Indicates that this task has finished executing.
     */
    void endTask();

    /**
     * Disposes any resources associated with this {@link TaskProgressMonitor}.
     * Callers should ensure that this is called when they are finished.
     * 
     * Callers should not dispose {@link TaskProgressMonitor}s that they do not
     * create - for example, those obtained by calling
     * {@link TaskProgressMonitor#newSubTask(int)} as their parents are
     * responsible for their lifecycle.
     */
    void dispose();
}
