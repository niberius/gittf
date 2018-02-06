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

import com.microsoft.gittf.core.tasks.framework.TaskCompletedHandler;
import com.microsoft.gittf.core.tasks.framework.TaskExecutor;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;

/**
 * Simple executor to run tasks from a command. Sets up defaults that are common
 * to executing tasks from commands:
 * 
 * Adds {@link CommonTaskExecutor#CONSOLE_OUTPUT_TASK_HANDLER} which outputs
 * task errors to standard error.
 * 
 * Adds {@link CommonTaskExecutor#LOGGING_TASK_HANDLER} as a start and completed
 * task handler, so that task results are logged.
 * 
 * Callers may remove these defaults by calling
 * {@link TaskExecutor#removeTaskStartedHandler(com.microsoft.gittf.core.tasks.framework.TaskStartedHandler)}
 * or {@link TaskExecutor#removeTaskCompletedHandler(TaskCompletedHandler)}.
 * 
 * @threadsafety unknown
 */
public class CommandTaskExecutor
    extends TaskExecutor
{
    public static final ConsoleOutputTaskHandler CONSOLE_OUTPUT_TASK_HANDLER = new ConsoleOutputTaskHandler();

    public static final LoggingTaskHandler LOGGING_TASK_HANDLER = new LoggingTaskHandler();

    public CommandTaskExecutor(final TaskProgressMonitor progressMonitor)
    {
        super(progressMonitor);

        addTaskStartedHandler(LOGGING_TASK_HANDLER);

        addTaskCompletedHandler(CONSOLE_OUTPUT_TASK_HANDLER);
        addTaskCompletedHandler(LOGGING_TASK_HANDLER);
    }
}
