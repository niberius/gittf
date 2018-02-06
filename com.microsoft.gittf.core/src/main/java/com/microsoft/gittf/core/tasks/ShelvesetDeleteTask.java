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

package com.microsoft.gittf.core.tasks;

import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.interfaces.VersionControlService;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskProgressDisplay;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;

/**
 * Deletes a shelveset
 * 
 */
public class ShelvesetDeleteTask
    extends Task
{
    private final VersionControlService versionControlService;
    private final String shelvesetName;
    private final String shelvesetOwnerName;

    /**
     * Constructor
     * 
     * @param versionControlService
     * @param shelvesetName
     * @param shelvesetOwnerName
     */
    public ShelvesetDeleteTask(
        final VersionControlService versionControlService,
        final String shelvesetName,
        final String shelvesetOwnerName)
    {
        Check.notNull(versionControlService, "versionControlService"); //$NON-NLS-1$
        Check.notNullOrEmpty(shelvesetName, "shelvesetName"); //$NON-NLS-1$

        this.versionControlService = versionControlService;
        this.shelvesetName = shelvesetName;
        this.shelvesetOwnerName = shelvesetOwnerName;
    }

    @Override
    public TaskStatus run(TaskProgressMonitor progressMonitor)
        throws Exception
    {
        progressMonitor.beginTask(Messages.getString("ShelvesetDeleteTask.DeletingShelveset"), //$NON-NLS-1$
            1,
            TaskProgressDisplay.DISPLAY_PROGRESS.combine(TaskProgressDisplay.DISPLAY_SUBTASK_DETAIL));

        /* Queries the server for matching shelvesets */
        Shelveset[] results = versionControlService.queryShelvesets(shelvesetName, shelvesetOwnerName);

        /* If there are no matching shelvesets error out */
        if (results.length == 0)
        {
            progressMonitor.endTask();
            return new TaskStatus(TaskStatus.ERROR, Messages.getString("ShelvesetDeleteTask.NoShelvesetsFound")); //$NON-NLS-1$
        }

        /* If there are more than one matching shelveset error out */
        if (results.length > 1)
        {
            progressMonitor.endTask();
            return new TaskStatus(TaskStatus.ERROR, Messages.getString("ShelvesetDeleteTask.MultipleShelvesetsFound")); //$NON-NLS-1$
        }

        /* Delete shelveset */
        versionControlService.deleteShelveset(results[0]);

        progressMonitor.endTask();

        progressMonitor.displayMessage(Messages.formatString("ShelvesetDeleteTask.ShlvesetDeletedSuccessfullyFormat", //$NON-NLS-1$
            results[0].getName()));

        return TaskStatus.OK_STATUS;
    }
}
