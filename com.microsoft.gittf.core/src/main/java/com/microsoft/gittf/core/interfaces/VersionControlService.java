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

import java.io.IOException;

import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

public interface VersionControlService
{
    Item getItem(String path, VersionSpec version, DeletedState deletedState, GetItemsOptions options);

    Item[] getItems(String path, ChangesetVersionSpec version, RecursionType recursion);

    void downloadFile(Item item, String downloadTo)
        throws IOException;

    void downloadShelvedFile(PendingChange shelvedChange, String downloadTo);

    void downloadBaseFile(PendingChange pendingChange, String downloadTo);

    Changeset getChangeset(int changesetID);

    Changeset[] queryHistory(
        String serverOrLocalPath,
        VersionSpec version,
        int deletionID,
        RecursionType recursion,
        java.lang.String user,
        VersionSpec versionFrom,
        VersionSpec versionTo,
        int maxCount,
        boolean includeFileDetails,
        boolean slotMode,
        boolean generateDownloadURLs,
        boolean sortAscending);

    Shelveset[] queryShelvesets(String shelvesetName, String shelvesetOwner);

    PendingSet[] queryShelvesetChanges(Shelveset shelveset, boolean includeDownloadInfo);

    void deleteShelveset(Shelveset shelveset);
}
