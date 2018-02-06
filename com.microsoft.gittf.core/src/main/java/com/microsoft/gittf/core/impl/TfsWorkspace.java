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

import com.microsoft.gittf.core.interfaces.WorkspaceService;
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
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;

/**
 * An implementation of the WorkspaceService that talks to a TFS workspace
 * 
 */
public class TfsWorkspace
    implements WorkspaceService
{
    private final Workspace workspace;

    /**
     * Constructor
     * 
     * @param workspace
     */
    public TfsWorkspace(Workspace workspace)
    {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;
    }

    public String getName()
    {
        return workspace.getName();
    }

    public void deleteWorkspace()
    {
        workspace.getClient().deleteWorkspace(workspace);
    }

    public int setLock(ItemSpec[] itemSpecs, LockLevel lockLevel, GetOptions getOptions, PendChangesOptions pendOptions)
    {
        return workspace.setLock(itemSpecs, lockLevel, getOptions, pendOptions);
    }

    public int pendAdd(
        String[] items,
        boolean recursive,
        FileEncoding fileEncoding,
        LockLevel lockLevel,
        GetOptions getOptions,
        PendChangesOptions pendOptions)
    {
        return workspace.pendAdd(items, recursive, fileEncoding, lockLevel, getOptions, pendOptions);
    }

    public int pendDelete(
        ItemSpec[] itemSpecs,
        LockLevel lockLevel,
        GetOptions getOptions,
        PendChangesOptions pendOptions)
    {
        return workspace.pendDelete(itemSpecs, lockLevel, getOptions, pendOptions);
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
        return workspace.pendEdit(itemSpecs, loclLevels, fileEncodings, getOptions, pendOptions, arg5);
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
        return workspace.pendRename(oldPaths, newPaths, lockLevel, getOptions, detectTargetItemType, pendOptions);
    }

    public int pendPropertyChange(
        final String path,
        final PropertyValue[] properties,
        final RecursionType recursion,
        final LockLevel lockLevel)
    {
        return workspace.pendPropertyChange(path, properties, recursion, lockLevel);
    }

    public void undo(ItemSpec[] itemSpecs)
    {
        workspace.undo(itemSpecs);
    }

    public void undo(ItemSpec[] itemSpecs, GetOptions getOptions)
    {
        workspace.undo(itemSpecs, getOptions);
    }

    public PendingSet getPendingChanges(String[] serverPaths, RecursionType recursionType, boolean includeDownloadInfo)
    {
        return workspace.getPendingChanges(serverPaths, recursionType, includeDownloadInfo);
    }

    public boolean canCheckIn()
    {
        return true;
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
        return workspace.checkIn(
            changes,
            author,
            authorDisplayName,
            fullMessage,
            checkinNote,
            associatedWorkItems,
            policyOverrideInfo,
            flags);
    }

    public int checkIn(
        PendingChange[] changes,
        String committer,
        String committerDisplayName,
        String author,
        String authorDisplayName,
        String fullMessage,
        CheckinNote checkinNote,
        WorkItemCheckinInfo[] associatedWorkItems,
        PolicyOverrideInfo policyOverrideInfo,
        CheckinFlags flags)
    {
        return workspace.checkIn(
            changes,
            committer,
            committerDisplayName,
            author,
            authorDisplayName,
            fullMessage,
            checkinNote,
            associatedWorkItems,
            policyOverrideInfo,
            flags);
    }

    public void shelve(Shelveset shelveset, PendingChange[] changes, boolean replace, boolean move)
    {
        workspace.shelve(shelveset, changes, replace, move);
    }

    public WorkspaceOperationErrorListener getErrorListener()
    {
        return new WorkspaceOperationErrorListener(workspace);
    }

    public IBuildServer getBuildServer()
    {
        return workspace.getClient().getConnection().getBuildServer();
    }

    public WebServiceLevel getServiceLevel()
    {
        return workspace.getClient().getServiceLevel();
    }
}
