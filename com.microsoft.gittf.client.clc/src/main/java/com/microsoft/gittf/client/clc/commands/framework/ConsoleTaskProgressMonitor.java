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

package com.microsoft.gittf.client.clc.commands.framework;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.gittf.client.clc.Console;
import com.microsoft.gittf.client.clc.Console.Verbosity;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.core.tasks.framework.BaseTaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.SubTaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskProgressDisplay;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.util.Check;

/**
 * An implementation of {@link TaskProgressMonitor} that will write status
 * messages to the console.
 * 
 * Three levels of verbosity exist:
 * 
 * {@link Verbosity#NORMAL} indicates that task execution reporting will occur
 * and that task messages will be displayed. (Verbose messages will not.)
 * 
 * {@link Verbosity#VERBOSE} indicates that task execution reporting will occur
 * and that all task messages and verbose messages will be displayed.
 * 
 * {@link Verbosity#QUIET} indicates that task execution reporting will
 * <i>not</i> occur and that task messages will not be displayed. (Warnings will
 * still be displayed.)
 * 
 * Tasks are displayed depending on whether they are to report progress or not.
 * 
 * By default,
 * {@link ConsoleTaskProgressMonitor#beginTask(String, int, TaskProgressDisplay)}
 * will simply display that given <code>task</code>. Any sub tasks created will
 * be given another {@link ConsoleTaskProgressMonitor}.
 * 
 * If, however,
 * {@link ConsoleTaskProgressMonitor#beginTask(String, int, TaskProgressDisplay)}
 * is called with {@link TaskProgressDisplay#DISPLAY_PROGRESS}, then progress
 * will be displayed and sub tasks will <i>not</i> have their tasks displayed -
 * instead, their progress reporting will simply roll up to this task. If a task
 * has an {@link TaskProgressMonitor#INDETERMINATE} work, then there will be no
 * percentage of progress displayed.
 * 
 */
public class ConsoleTaskProgressMonitor
    extends BaseTaskProgressMonitor
{
    private static final Log log = LogFactory.getLog(ConsoleTaskProgressMonitor.class);

    private final Console console;

    private static final int CONSOLE_TRUNCATION_PADDING;

    private boolean inTask = false;
    private String task;
    private int workTotal;
    private TaskProgressDisplay displayOptions = TaskProgressDisplay.NONE;
    private double worked;
    private String detail = null;

    private String progressLine = ""; //$NON-NLS-1$

    private final List<TaskProgressMonitor> subMonitors = new ArrayList<TaskProgressMonitor>();

    static
    {
        /*
         * Measure how many characters we add for the trailing ... to indicate
         * truncation
         */
        CONSOLE_TRUNCATION_PADDING =
            Messages.formatString("ConsoleTaskProgressMonitor.TaskTruncationFormat", "").length(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public ConsoleTaskProgressMonitor(final Console console)
    {
        Check.notNull(console, "console"); //$NON-NLS-1$

        this.console = console;
    }

    public void beginTask(final String task, final int workTotal, final TaskProgressDisplay displayOptions)
    {
        Check.notNull(task, "task"); //$NON-NLS-1$
        Check.isTrue(workTotal >= INDETERMINATE, "work >= INDETERMINATE"); //$NON-NLS-1$
        Check.notNull(displayOptions, "displayOptions"); //$NON-NLS-1$

        log.info(task + " started"); //$NON-NLS-1$

        if (this.inTask)
        {
            return;
        }

        this.inTask = true;
        this.task = task;
        this.workTotal = workTotal;
        this.displayOptions = displayOptions;
        this.worked = 0;

        /*
         * If we're displaying progress, let the updateProgress routine handle
         * all the display.
         */
        if (this.displayOptions.contains(TaskProgressDisplay.DISPLAY_PROGRESS))
        {
            startProgress();
        }
        /* Otherwise, just print that the task started. */
        else if (console.getVerbosity() != Verbosity.QUIET)
        {
            console.getOutputStream().println(task);
        }
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

    public TaskProgressMonitor newSubTask(final int subWork)
    {
        final TaskProgressMonitor subMonitor;

        /*
         * If we're currently displaying progress for this task, then the task
         * from the submonitor should be *ignored* - instead we should just
         * proxy up their work so that we can update the percentage.
         */
        if (this.displayOptions.contains(TaskProgressDisplay.DISPLAY_PROGRESS))
        {
            subMonitor = new SubTaskProgressMonitor(this, subWork);
        }
        /*
         * Otherwise, just let it print status like it always would.
         */
        else
        {
            subMonitor = new ConsoleTaskProgressMonitor(console);
        }

        subMonitors.add(subMonitor);

        return subMonitor;
    }

    public void setDetail(final String detail)
    {
        this.detail = detail;
        updateProgress();
    }

    public String getDetail()
    {
        return detail;
    }

    public void worked(final double amount)
    {
        if (!this.inTask)
        {
            return;
        }

        final double newWorked = this.worked + amount;
        this.worked = newWorked > this.workTotal ? this.workTotal : newWorked;

        updateProgress();
    }

    public void displayMessage(final String message)
    {
        if (console.getVerbosity() != Verbosity.QUIET)
        {
            clearProgressLine();
            console.getOutputStream().println(message);
            updateProgress();
        }
    }

    public void displayWarning(final String message)
    {
        clearProgressLine();
        console.getErrorStream().println(Messages.formatString("Main.WarningFormat", message)); //$NON-NLS-1$
        updateProgress();
    }

    public void displayVerbose(final String message)
    {
        if (console.getVerbosity() == Verbosity.VERBOSE)
        {
            clearProgressLine();
            console.getOutputStream().println(message);
            updateProgress();
        }
    }

    private void clearProgressLine()
    {
        for (int i = 0; i < progressLine.length(); i++)
        {
            console.getOutputStream().print('\b');
        }
        for (int i = 0; i < progressLine.length(); i++)
        {
            console.getOutputStream().print(' ');
        }
        for (int i = 0; i < progressLine.length(); i++)
        {
            console.getOutputStream().print('\b');
        }

        progressLine = ""; //$NON-NLS-1$
    }

    private void startProgress()
    {
        if (console.getVerbosity() != Verbosity.QUIET
            && this.displayOptions.contains(TaskProgressDisplay.DISPLAY_PROGRESS))
        {
            if (this.workTotal == TaskProgressMonitor.INDETERMINATE || !console.supportsOverwrite())
            {
                console.getOutputStream().print(
                    Messages.formatString("ConsoleTaskProgressMonitor.IndeterminateTaskStartedFormat", task)); //$NON-NLS-1$
                console.getOutputStream().flush();
            }
            else
            {
                updateProgress();
            }
        }
    }

    private void updateProgress()
    {
        if (console.getVerbosity() != Verbosity.QUIET
            && this.displayOptions.contains(TaskProgressDisplay.DISPLAY_PROGRESS)
            && this.workTotal > TaskProgressMonitor.INDETERMINATE
            && console.supportsOverwrite())
        {
            String newProgressLine;
            if (detail != null && detail.length() > 0)
            {
                newProgressLine = Messages.formatString("ConsoleTaskProgressMonitor.TaskProgressWithDetailFormat", //$NON-NLS-1$
                    task,
                    Integer.toString((int) ((worked / workTotal) * 100)),
                    detail);
            }
            else
            {
                newProgressLine = Messages.formatString("ConsoleTaskProgressMonitor.TaskProgressFormat", //$NON-NLS-1$
                    task,
                    Integer.toString((int) ((worked / workTotal) * 100)));
            }

            /* Truncate overly long progress lines */
            final int consoleWidth = console.getWidth();

            if (newProgressLine.length() > (consoleWidth - 1))
            {
                newProgressLine = Messages.formatString("ConsoleTaskProgressMonitor.TaskTruncationFormat", //$NON-NLS-1$
                    newProgressLine.substring(0, (consoleWidth - 1) - CONSOLE_TRUNCATION_PADDING));
            }

            writeProgressLine(newProgressLine);
            progressLine = newProgressLine;
        }
    }

    /**
     * Write the progress line intelligently to avoid flicker on slow console
     * implementations. (We don't need to overwrite any characters that are
     * already on the progress line.)
     * 
     * @param newProgressLine
     */
    private void writeProgressLine(final String newProgressLine)
    {
        log.info(newProgressLine);

        /* Remove any characters at the end of the line */
        if (newProgressLine.length() < progressLine.length())
        {
            int trailing = progressLine.length() - newProgressLine.length();

            clearProgressChars(trailing);
            progressLine = progressLine.substring(0, newProgressLine.length());
        }

        /* Determine the longest substring between the old line and the new */
        int common = getCommonSubstring(progressLine, newProgressLine);

        reverseProgressChars(progressLine.length() - common);
        console.getOutputStream().print(newProgressLine.substring(common, newProgressLine.length()));
        console.getOutputStream().flush();
    }

    private void clearProgressChars(int count)
    {
        for (int i = 0; i < count; i++)
        {
            console.getOutputStream().print('\b');
        }
        for (int i = 0; i < count; i++)
        {
            console.getOutputStream().print(' ');
        }
        for (int i = 0; i < count; i++)
        {
            console.getOutputStream().print('\b');
        }
    }

    private void reverseProgressChars(int count)
    {
        for (int i = 0; i < count; i++)
        {
            console.getOutputStream().print('\b');
        }
    }

    private static int getCommonSubstring(final String one, final String two)
    {
        Check.notNull(one, "one"); //$NON-NLS-1$
        Check.notNull(two, "two"); //$NON-NLS-1$

        int i;

        for (i = 0; i < one.length() && i < two.length(); i++)
        {
            if (one.charAt(i) != two.charAt(i))
            {
                break;
            }
        }

        return i;
    }

    private void finishProgress()
    {
        if (console.getVerbosity() != Verbosity.QUIET
            && this.displayOptions.contains(TaskProgressDisplay.DISPLAY_PROGRESS))
        {
            if (this.workTotal == TaskProgressMonitor.INDETERMINATE || !console.supportsOverwrite())
            {
                console.getOutputStream().println(Messages.getString("ConsoleTaskProgressMonitor.TaskProgressDone")); //$NON-NLS-1$
                console.getOutputStream().flush();
            }
            else
            {
                clearProgressLine();
                console.getOutputStream().println(
                    Messages.formatString("ConsoleTaskProgressMonitor.TaskProgressWithDetailFormat", //$NON-NLS-1$
                        task,
                        100,
                        Messages.getString("ConsoleTaskProgressMonitor.TaskProgressDone"))); //$NON-NLS-1$
                console.getOutputStream().flush();
            }
        }
    }

    public void endTask()
    {
        log.info(task + " ended"); //$NON-NLS-1$

        try
        {
            finishProgress();
        }
        finally
        {
            this.inTask = false;
            this.task = null;
            this.workTotal = 0;
            this.displayOptions = TaskProgressDisplay.NONE;
            this.worked = 0;
            this.detail = null;
        }
    }

    public void dispose()
    {
        /*
         * If there's still something on the display, we should print a newline
         * (we likely caught an exception.)
         */
        if (console.getVerbosity() != Verbosity.QUIET
            && this.task != null
            && this.displayOptions.contains(TaskProgressDisplay.DISPLAY_PROGRESS))
        {
            console.getOutputStream().println();
            console.getOutputStream().flush();
        }

        for (TaskProgressMonitor subMonitor : subMonitors)
        {
            try
            {
                subMonitor.dispose();
            }
            catch (Exception e)
            {
                log.warn("Exception disposing progress monitor", e); //$NON-NLS-1$
            }
        }
    }
}
