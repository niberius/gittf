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
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Represents the task execution status
 * 
 */
public class TaskStatus
{
    /**
     * Task executed successfully
     */
    public static final TaskStatusSeverity OK = new TaskStatusSeverity(0);

    /**
     * Task executed successfully with some info
     */
    public static final TaskStatusSeverity INFO = new TaskStatusSeverity(1);

    /**
     * Task executed successfully with some warnings
     */
    public static final TaskStatusSeverity WARNING = new TaskStatusSeverity(2);

    /**
     * Task failed
     */
    public static final TaskStatusSeverity ERROR = new TaskStatusSeverity(3);

    /**
     * The task was cancelled
     */
    public static final TaskStatusSeverity CANCEL = new TaskStatusSeverity(4);

    /**
     * An OK Status
     */
    public static final TaskStatus OK_STATUS = new TaskStatus(OK);

    private final TaskStatusSeverity severity;
    private final int code;
    private final String message;
    private final Exception exception;

    /**
     * Constructor
     * 
     * @param severity
     */
    public TaskStatus(final TaskStatusSeverity severity)
    {
        this(severity, 0, null, null);
    }

    /**
     * Constructor
     * 
     * @param severity
     * @param code
     */
    public TaskStatus(final TaskStatusSeverity severity, int code)
    {
        this(severity, code, null, null);
    }

    /**
     * Constructor
     * 
     * @param severity
     * @param message
     */
    public TaskStatus(final TaskStatusSeverity severity, final String message)
    {
        this(severity, 0, message, null);
    }

    /**
     * Constructor
     * 
     * @param severity
     * @param code
     * @param message
     */
    public TaskStatus(final TaskStatusSeverity severity, int code, final String message)
    {
        this(severity, code, message, null);
    }

    /**
     * Constructor
     * 
     * @param severity
     * @param exception
     */
    public TaskStatus(final TaskStatusSeverity severity, final Exception exception)
    {
        this(severity, 0, null, exception);
    }

    /**
     * Constructor
     * 
     * @param severity
     * @param code
     * @param exception
     */
    public TaskStatus(final TaskStatusSeverity severity, int code, final Exception exception)
    {
        this(severity, code, null, exception);
    }

    /**
     * Constructor
     * 
     * @param severity
     * @param code
     * @param message
     * @param exception
     */
    public TaskStatus(final TaskStatusSeverity severity, final int code, final String message, final Exception exception)
    {
        Check.notNull(severity, "severity"); //$NON-NLS-1$

        this.severity = severity;
        this.code = code;
        this.message = message;
        this.exception = exception;
    }

    /**
     * Gets the severity
     * 
     * @return
     */
    public TaskStatusSeverity getSeverity()
    {
        return severity;
    }

    /**
     * Gets the code
     * 
     * @return
     */
    public int getCode()
    {
        return code;
    }

    /**
     * Gets the Message
     * 
     * @return
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Gets the Exception
     * 
     * @return
     */
    public Exception getException()
    {
        return exception;
    }

    /**
     * Returns true if the task execution was OK
     * 
     * @return
     */
    public boolean isOK()
    {
        return TaskStatus.OK.equals(severity);
    }

    /**
     * The task severity enum
     * 
     */
    public static class TaskStatusSeverity
        extends TypesafeEnum
    {
        private TaskStatusSeverity(int value)
        {
            super(value);
        }
    }
}
