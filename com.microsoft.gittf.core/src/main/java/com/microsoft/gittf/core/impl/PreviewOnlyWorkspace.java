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

package com.microsoft.gittf.core.impl;

import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.interfaces.WorkspaceService;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.WorkspaceOperationErrorListener;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.versioncontrol.CheckinFlags;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyOverrideInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;

/**
 * Workspace object used to display the pending changes pended only
 * 
 */
public class PreviewOnlyWorkspace
    implements WorkspaceService
{
    private final TaskProgressMonitor progressMonitor;

    /**
     * Constructor
     * 
     * TODO : This class needs to call an interface and have the CLC project
     * display changes on the command line *
     * 
     * @param progressMonitor
     *        the progress monitor used to display the pending change
     */
    public PreviewOnlyWorkspace(TaskProgressMonitor progressMonitor)
    {
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        this.progressMonitor = progressMonitor;
    }

    public String getName()
    {
        return "PreviewOnlyWorkspace"; //$NON-NLS-1$
    }

    public void deleteWorkspace()
    {
    }

    public int setLock(ItemSpec[] itemSpecs, LockLevel loclLevel, GetOptions getOptions, PendChangesOptions pendOptions)
    {
        return itemSpecs.length;
    }

    public int pendAdd(
        String[] items,
        boolean recursive,
        FileEncoding fileEncoding,
        LockLevel lockLevel,
        GetOptions getOptions,
        PendChangesOptions pendOptions)
    {
        for (String item : items)
        {
            progressMonitor.displayMessage(Messages.formatString("PreviewOnlyWorkspace.AddFormat", item)); //$NON-NLS-1$
        }

        return items.length;
    }

    public int pendDelete(
        ItemSpec[] itemSpecs,
        LockLevel lockLevel,
        GetOptions getOptions,
        PendChangesOptions pendOptions)
    {
        for (ItemSpec item : itemSpecs)
        {
            progressMonitor.displayMessage(Messages.formatString("PreviewOnlyWorkspace.DeleteFormat", item.getItem())); //$NON-NLS-1$
        }

        return itemSpecs.length;
    }

    public int pendEdit(
        ItemSpec[] itemSpecs,
        LockLevel[] loclLevels,
        FileEncoding[] fileEncodings,
        GetOptions getOptions,
        PendChangesOptions pendOptions,
        String[] arg5,
        boolean display)
    {
        if (display)
        {
            for (ItemSpec item : itemSpecs)
            {
                progressMonitor.displayMessage(Messages.formatString("PreviewOnlyWorkspace.EditFormat", item.getItem())); //$NON-NLS-1$
            }
        }

        return itemSpecs.length;
    }

    public int pendRename(
        String[] oldPaths,
        String[] newPaths,
        Boolean[] editFlag,
        LockLevel lockLevel,
        GetOptions getOptions,
        boolean detectTargetItemType,
        PendChangesOptions pendOptions)
    {
        for (int count = 0; count < oldPaths.length; count++)
        {
            if (editFlag != null && editFlag[count])
            {
                progressMonitor.displayMessage(Messages.formatString(
                    "PreviewOnlyWorkspace.RenameEditFormat", newPaths[count])); //$NON-NLS-1$
            }
            else
            {
                progressMonitor.displayMessage(Messages.formatString(
                    "PreviewOnlyWorkspace.RenameFormat", newPaths[count])); //$NON-NLS-1$
            }
        }

        return oldPaths.length;
    }

    public int pendPropertyChange(
        final String path,
        final PropertyValue[] properties,
        final RecursionType recursion,
        final LockLevel lockLevel)
    {
        return 1;
    }

    public void undo(ItemSpec[] itemSpecs)
    {

    }

    public void undo(ItemSpec[] itemSpecs, GetOptions getOptions)
    {

    }

    public PendingSet getPendingChanges(String[] serverPaths, RecursionType recursionType, boolean includeDownloadInfo)
    {
        return null;
    }

    public boolean canCheckIn()
    {
        return false;
    }

    public int checkIn(
        PendingChange[] changes,
        String author,
        String authorDisplayName,
        String fullMessage,
        CheckinNote checkinNote,
        WorkItemCheckinInfo[] associatedWorkItems,
        PolicyOverrideInfo policyOverrideInfo,
        CheckinFlags flags)
    {
        return 0;
    }

    public int checkIn(
        PendingChange[] changes,
        String author,
        String authorDisplayName,
        String committer,
        String committerDisplayName,
        String fullMessage,
        CheckinNote checkinNote,
        WorkItemCheckinInfo[] associatedWorkItems,
        PolicyOverrideInfo policyOverrideInfo,
        CheckinFlags flags)
    {
        return 0;
    }

    public void shelve(Shelveset shelveset, PendingChange[] changes, boolean replace, boolean move)
    {

    }

    public WorkspaceOperationErrorListener getErrorListener()
    {
        return WorkspaceOperationErrorListener.EMPTY;
    }

    public IBuildServer getBuildServer()
    {
        return null;
    }

    public WebServiceLevel getServiceLevel()
    {
        return WebServiceLevel.TFS_2012_QU1;
    }
}
