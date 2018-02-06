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

import com.microsoft.gittf.client.clc.Main;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskCompletedHandler;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;

public class ConsoleOutputTaskHandler
    implements TaskCompletedHandler
{
    public void onTaskCompleted(final Task task, final TaskStatus status)
    {
        String message;

        if (status.getSeverity() == TaskStatus.ERROR)
        {
            if (status.getMessage() != null && status.getException() != null)
            {
                message = Messages.formatString("ConsoleOutputTaskHandler.ExtendedErrorFormat", //$NON-NLS-1$
                    status.getMessage(),
                    status.getException().getLocalizedMessage());
            }
            else if (status.getMessage() != null)
            {
                message = status.getMessage();
            }
            else if (status.getException() != null)
            {
                message = Messages.formatString("ConsoleOutputTaskHandler.ExceptionFormat", //$NON-NLS-1$
                    status.getException().getLocalizedMessage());
            }
            else
            {
                message = Messages.getString("ConsoleOutputTaskHandler.UnknownError"); //$NON-NLS-1$
            }

            Main.printError(message);
        }
    }
}