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

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.gittf.client.clc.ProductInformation;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskCompletedHandler;
import com.microsoft.gittf.core.tasks.framework.TaskStartedHandler;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;

public class LoggingTaskHandler
    implements TaskStartedHandler, TaskCompletedHandler
{
    private static final Log log = LogFactory.getLog(ProductInformation.getProductName());

    public void onTaskStarted(Task task)
    {
        log.debug(MessageFormat.format("Starting task {0}", task.getClass().getSimpleName())); //$NON-NLS-1$
    }

    public void onTaskCompleted(Task task, TaskStatus status)
    {
        if (status.getSeverity() == TaskStatus.ERROR && log.isErrorEnabled())
        {
            log.error(getMessage(task, status), status.getException());
        }
        else if (status.getSeverity() == TaskStatus.WARNING && log.isWarnEnabled())
        {
            log.warn(getMessage(task, status), status.getException());
        }
        else if ((status.getSeverity() == TaskStatus.CANCEL && log.isInfoEnabled())
            || status.getSeverity() == TaskStatus.INFO
            && log.isInfoEnabled())
        {
            log.info(getMessage(task, status), status.getException());
        }
        else if (log.isDebugEnabled())
        {
            log.debug(getMessage(task, status), status.getException());
        }
    }

    private static String getMessage(final Task task, final TaskStatus status)
    {
        final String prefix;

        if (status.getSeverity() == TaskStatus.ERROR)
        {
            prefix = "Error executing"; //$NON-NLS-1$
        }
        else if (status.getSeverity() == TaskStatus.WARNING)
        {
            prefix = "Warning while executing"; //$NON-NLS-1$
        }
        else if (status.getSeverity() == TaskStatus.CANCEL)
        {
            prefix = "Cancelled execution of"; //$NON-NLS-1$
        }
        else
        {
            prefix = "Completed"; //$NON-NLS-1$
        }

        if (status.getMessage() == null)
        {
            return MessageFormat.format("{0} task {1}", prefix, task.getClass().getSimpleName()); //$NON-NLS-1$
        }
        else
        {
            return MessageFormat.format(
                "{0} task {1}: {2}", prefix, task.getClass().getSimpleName(), status.getMessage()); //$NON-NLS-1$
        }
    }
}
