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

import java.io.IOException;

import com.microsoft.gittf.core.interfaces.VersionControlService;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

/**
 * An implementation of the VersionControlService that talks to the real TFS
 * Server
 * 
 */
public class TfsVersionControlService
    implements VersionControlService
{

    private final VersionControlClient versionControlClient;

    /**
     * Constructor
     * 
     * @param versionControlClient
     *        the version contol client object to use
     */
    public TfsVersionControlService(VersionControlClient versionControlClient)
    {
        Check.notNull(versionControlClient, "versionControlClient"); //$NON-NLS-1$

        this.versionControlClient = versionControlClient;
    }

    public Item getItem(String path, VersionSpec version, DeletedState deletedState, GetItemsOptions options)
    {
        return versionControlClient.getItem(path, version, deletedState, options);
    }

    public Item[] getItems(String path, ChangesetVersionSpec version, RecursionType recursion)
    {
        return versionControlClient.getItems(path, version, recursion, DeletedState.NON_DELETED, ItemType.ANY, true).getItems();
    }

    public void downloadFile(Item item, String downloadTo)
        throws IOException
    {
        item.downloadFile(versionControlClient, downloadTo);
    }

    public void downloadShelvedFile(PendingChange shelvedChange, String downloadTo)
    {
        shelvedChange.downloadShelvedFile(versionControlClient, downloadTo);
    }

    public void downloadBaseFile(PendingChange pendingChange, String downloadTo)
    {
        pendingChange.downloadBaseFile(versionControlClient, downloadTo);
    }

    public Changeset getChangeset(int changesetID)
    {
        return versionControlClient.getChangeset(changesetID);
    }

    public Changeset[] queryHistory(
        String serverOrLocalPath,
        VersionSpec version,
        int deletionID,
        RecursionType recursion,
        String user,
        VersionSpec versionFrom,
        VersionSpec versionTo,
        int maxCount,
        boolean includeFileDetails,
        boolean slotMode,
        boolean generateDownloadURLs,
        boolean sortAscending)
    {
        return versionControlClient.queryHistory(
            serverOrLocalPath,
            version,
            deletionID,
            recursion,
            user,
            versionFrom,
            versionTo,
            maxCount,
            includeFileDetails,
            slotMode,
            generateDownloadURLs,
            sortAscending);
    }

    public Shelveset[] queryShelvesets(String shelvesetName, String shelvesetOwner)
    {
        return versionControlClient.queryShelvesets(shelvesetName, shelvesetOwner, null);
    }

    public PendingSet[] queryShelvesetChanges(Shelveset shelveset, boolean includeDownloadInfo)
    {
        return versionControlClient.queryShelvedChanges(
            shelveset.getName(),
            shelveset.getOwnerName(),
            null,
            includeDownloadInfo);
    }

    public void deleteShelveset(Shelveset shelveset)
    {
        versionControlClient.deleteShelveset(shelveset.getName(), shelveset.getOwnerName());
    }
}
