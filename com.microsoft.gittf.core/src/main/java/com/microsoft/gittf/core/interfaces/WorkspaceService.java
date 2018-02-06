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

package com.microsoft.gittf.core.interfaces;

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

public interface WorkspaceService
{
    String getName();

    void deleteWorkspace();

    int setLock(ItemSpec[] itemSpecs, LockLevel loclLevel, GetOptions getOptions, PendChangesOptions pendOptions);

    int pendAdd(
        String[] items,
        boolean recursive,
        FileEncoding fileEncoding,
        LockLevel lockLevel,
        GetOptions getOptions,
        PendChangesOptions pendOptions);

    int pendDelete(ItemSpec[] itemSpecs, LockLevel lockLevel, GetOptions getOptions, PendChangesOptions pendOptions);

    int pendEdit(
        ItemSpec[] itemSpecs,
        LockLevel[] loclLevels,
        FileEncoding[] fileEncodings,
        GetOptions getOptions,
        PendChangesOptions pendOptions,
        String[] arg5,
        boolean display);

    int pendRename(
        String[] oldPaths,
        String[] newPaths,
        Boolean[] editFlag,
        LockLevel lockLevel,
        GetOptions getOptions,
        boolean detectTargetItemType,
        PendChangesOptions pendOptions);

    int pendPropertyChange(String path, PropertyValue[] properties, RecursionType recursion, LockLevel lockLevel);

    void undo(ItemSpec[] itemSpecs);

    void undo(ItemSpec[] itemSpecs, GetOptions getOptions);

    PendingSet getPendingChanges(String[] serverPaths, RecursionType recursionType, boolean includeDownloadInfo);

    boolean canCheckIn();

    int checkIn(
        PendingChange[] changes,
        String author,
        String authorDisplayName,
        String fullMessage,
        CheckinNote checkinNote,
        WorkItemCheckinInfo[] associatedWorkItems,
        PolicyOverrideInfo policyOverrideInfo,
        CheckinFlags flags);

    int checkIn(
        PendingChange[] changes,
        String author,
        String authorDisplayName,
        String committer,
        String committerDisplayName,
        String fullMessage,
        CheckinNote checkinNote,
        WorkItemCheckinInfo[] associatedWorkItems,
        PolicyOverrideInfo policyOverrideInfo,
        CheckinFlags flags);

    void shelve(Shelveset shelveset, PendingChange[] changes, boolean replace, boolean move);

    WorkspaceOperationErrorListener getErrorListener();

    IBuildServer getBuildServer();

    public WebServiceLevel getServiceLevel();
}
