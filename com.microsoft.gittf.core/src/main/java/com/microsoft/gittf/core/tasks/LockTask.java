/*
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
 */

package com.microsoft.gittf.core.tasks;

import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.interfaces.WorkspaceService;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LockTask
        extends Task {
    private static final Log log = LogFactory.getLog(LockTask.class);

    private final WorkspaceService workspace;
    private final String serverPath;

    public LockTask(final WorkspaceService workspace, final String serverPath) {
        Check.notNull(workspace, "workspace");
        Check.notNullOrEmpty(serverPath, "serverPath");

        this.workspace = workspace;
        this.serverPath = serverPath;
    }

    @Override
    public TaskStatus run(final TaskProgressMonitor progressMonitor) {
        progressMonitor.beginTask(
                Messages.formatString("LockTask.LockingFormat", serverPath), TaskProgressMonitor.INDETERMINATE);

        log.debug("Trying to lock " + serverPath);

        int pended = workspace.setLock(new ItemSpec[]
                {
                        new ItemSpec(serverPath, RecursionType.FULL)
                }, LockLevel.CHECKIN, GetOptions.NO_DISK_UPDATE, PendChangesOptions.NONE);

        /*
         * If we cannot lock the item because it does not exist, we pend an add
         * for the root folder and lock it
         */
        if (pended == 0) {

            log.debug("Cannot lock " + serverPath + "because it does not exist");

            log.debug("Trying to pend add for " + serverPath);

            pended = workspace.pendAdd(new String[]
                    {
                            serverPath
                    }, true, null, LockLevel.CHECKIN, GetOptions.NO_DISK_UPDATE, PendChangesOptions.NONE);

            if (pended != 1) {
                return new TaskStatus(TaskStatus.ERROR, Messages.formatString("LockTask.LockFailedFormat", serverPath));
            }
        }

        progressMonitor.endTask();

        return TaskStatus.OK_STATUS;
    }
}
