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

package com.microsoft.gittf.core.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.microsoft.gittf.core.OutputConstants;
import com.microsoft.gittf.core.interfaces.VersionControlService;
import com.microsoft.gittf.core.test.Util;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

public class MockVersionControlService
    implements VersionControlService
{
    private static final int INVALID_CHANGESET_NUMBER = -1;

    private HashMap<Integer, HashSet<String>> itemData = new HashMap<Integer, HashSet<String>>();
    private HashMap<Integer, MockChangesetProperties> changesetData = new HashMap<Integer, MockChangesetProperties>();

    private int latestChangeset;

    public Item getItem(String serverPath, VersionSpec version, DeletedState deletedState, GetItemsOptions options)
    {
        // only changeset version or latest version are accepted
        // deleted state and get options are ignored

        int versionChangesetNumber = getChangesetNumberFromVersion(version);
        if (versionChangesetNumber == INVALID_CHANGESET_NUMBER)
        {
            return null;
        }

        for (int backwardChangesetCounter = versionChangesetNumber; backwardChangesetCounter > 0; backwardChangesetCounter--)
        {
            if (!itemData.containsKey(new Integer(versionChangesetNumber)))
            {
                continue;
            }

            HashSet<String> changesetData = itemData.get(new Integer(backwardChangesetCounter));

            if (!DoesChangesetDataHasServerPath(changesetData, serverPath))
            {
                continue;
            }

            Item toReturn = new Item();
            toReturn.setChangeSetID(backwardChangesetCounter);
            toReturn.setServerItem(serverPath);
            toReturn.setItemType(ItemType.FOLDER);

            return toReturn;
        }

        return null;
    }

    public Item[] getItems(String path, ChangesetVersionSpec version, RecursionType recursion)
    {
        // recursion is ignored

        int versionChangesetNumber = getChangesetNumberFromVersion(version);
        if (versionChangesetNumber == INVALID_CHANGESET_NUMBER)
        {
            return null;
        }

        ArrayList<Item> toReturn = new ArrayList<Item>();
        HashMap<String, Integer> itemInList = new HashMap<String, Integer>();

        for (int backwardChangesetCounter = versionChangesetNumber; backwardChangesetCounter > 0; backwardChangesetCounter--)
        {
            if (!itemData.containsKey(new Integer(backwardChangesetCounter)))
            {
                continue;
            }

            HashSet<String> changesetData = itemData.get(new Integer(backwardChangesetCounter));

            Item[] itemsInChangeset = getItemsUnderPathFromChangeset(path, changesetData, backwardChangesetCounter);
            for (Item itemInChangeset : itemsInChangeset)
            {
                if (itemInList.containsKey(itemInChangeset.getServerItem()))
                {
                    continue;
                }

                toReturn.add(itemInChangeset);
                itemInList.put(itemInChangeset.getServerItem(), new Integer(itemInChangeset.getChangeSetID()));
            }
        }

        Item[] items = new Item[toReturn.size()];
        return toReturn.toArray(items);
    }

    public void downloadFile(Item item, String downloadTo)
        throws IOException
    {
        FileWriter fw = new FileWriter(new File(downloadTo));

        fw.write(generatFileContent(item));

        fw.close();
    }

    public void downloadShelvedFile(PendingChange shelvedChange, String downloadTo)
    {

    }

    public void downloadBaseFile(PendingChange pendingChange, String downloadTo)
    {

    }

    public Changeset getChangeset(int changesetID)
    {
        if (changesetID > 0 && changesetID <= latestChangeset)
        {
            Changeset change = new Changeset();
            change.setChangesetID(changesetID);
            UpdateChangesetOption(change);

            return change;
        }

        return null;
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
        // Only accepted parameters are:
        // - serverOrLocalPath : server Only
        // - version
        // - versionFrom
        // - versionTo
        // - maxCount

        ArrayList<Changeset> toReturn = new ArrayList<Changeset>();

        int versionChangesetNumber = getChangesetNumberFromVersion(version);
        if (versionChangesetNumber != INVALID_CHANGESET_NUMBER)
        {
            int versionFromChangeSetNumber = getChangesetNumberFromVersion(versionFrom);
            if (versionFromChangeSetNumber == INVALID_CHANGESET_NUMBER)
            {
                versionFromChangeSetNumber = 0;
            }

            int versionToChangeSetNumber = getChangesetNumberFromVersion(versionTo);
            if (versionToChangeSetNumber == INVALID_CHANGESET_NUMBER)
            {
                versionToChangeSetNumber = latestChangeset;
            }

            for (int backwardChangesetCounter = latestChangeset; backwardChangesetCounter > 0; backwardChangesetCounter--)
            {
                if (toReturn.size() >= maxCount)
                {
                    break;
                }

                if (backwardChangesetCounter < versionFromChangeSetNumber)
                {
                    break;
                }

                if (backwardChangesetCounter > versionToChangeSetNumber)
                {
                    continue;
                }

                if (backwardChangesetCounter > versionChangesetNumber)
                {
                    continue;
                }

                if (!itemData.containsKey(new Integer(backwardChangesetCounter)))
                {
                    continue;
                }

                HashSet<String> changesetData = itemData.get(new Integer(backwardChangesetCounter));
                if (!DoesChangesetDataHasServerPath(changesetData, serverOrLocalPath))
                {
                    continue;
                }

                Changeset change = new Changeset();
                change.setChangesetID(backwardChangesetCounter);
                UpdateChangesetOption(change);

                toReturn.add(change);
            }
        }

        Changeset[] changesets = new Changeset[toReturn.size()];
        return toReturn.toArray(changesets);
    }

    public void AddFile(String serverPath, int changesetId)
    {
        if (itemData.containsKey(new Integer(changesetId)))
        {
            HashSet<String> changesetData = itemData.get(new Integer(changesetId));
            changesetData.add(serverPath);
        }
        else
        {
            HashSet<String> changesetData = new HashSet<String>();
            changesetData.add(serverPath);

            itemData.put(new Integer(changesetId), changesetData);

            if (changesetId > latestChangeset)
            {
                latestChangeset = changesetId;
            }
        }
    }

    public void updateChangesetInformation(MockChangesetProperties changesetProperties, int changesetId)
    {
        changesetData.put(new Integer(changesetId), changesetProperties);
    }

    public boolean verifyFileContent(byte[] fileContent, String serverPath, int changesetId)
    {
        try
        {
            String actualFileContent = new String(fileContent);
            String expectedContent = generateFileContent(serverPath, changesetId);
            return actualFileContent.equals(expectedContent);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public boolean verifyFileContent(File localFile, String serverPath, int changesetId)
    {
        return Util.verifyFileContent(localFile, generateFileContent(serverPath, changesetId));
    }

    private void UpdateChangesetOption(Changeset change)
    {
        if (changesetData.containsKey(new Integer(change.getChangesetID())))
        {
            MockChangesetProperties properties = changesetData.get(new Integer(change.getChangesetID()));

            change.setOwner(properties.getownerName());
            change.setOwnerDisplayName(properties.getownerDisplayName());
            change.setCommitter(properties.getcommitterName());
            change.setCommitterDisplayName(properties.getcommitterDisplayName());

            change.setComment(properties.getComment());
            change.setDate(properties.getDate());
        }
    }

    private int getChangesetNumberFromVersion(VersionSpec version)
    {
        if (version instanceof LatestVersionSpec)
        {
            return latestChangeset;
        }
        else if (version instanceof ChangesetVersionSpec)
        {
            return ((ChangesetVersionSpec) version).getChangeset();
        }
        else
        {
            return INVALID_CHANGESET_NUMBER;
        }
    }

    private String generatFileContent(Item item)
    {
        return generateFileContent(item.getServerItem(), item.getChangeSetID());
    }

    private String generateFileContent(String path, int changeset)
    {
        StringBuilder sb = new StringBuilder();

        for (int count = 0; count < 10; count++)
        {
            sb.append(String.format("FilePath : %s - Version : %s .", path, Integer.toString(changeset)));//$NON-NLS-1$
            sb.append(OutputConstants.NEW_LINE);
        }

        return sb.toString();
    }

    private boolean DoesChangesetDataHasServerPath(HashSet<String> changesetData, String serverOrLocalPath)
    {
        String serverPath = serverOrLocalPath.replace('*', ' ').trim();
        serverPath =
            (serverPath.endsWith("/") || serverPath.endsWith("\\")) ? serverPath.substring(0, serverPath.length() - 1) //$NON-NLS-1$ //$NON-NLS-2$
                : serverPath;

        if (changesetData.contains(serverPath))
        {
            return true;
        }

        for (String data : changesetData)
        {
            if (data.startsWith(serverPath))
            {
                return true;
            }
        }

        return false;
    }

    private Item[] getItemsUnderPathFromChangeset(String path, HashSet<String> changesetData, int changesetNumber)
    {
        String serverPath = path.replace('*', ' ').trim();
        serverPath =
            (serverPath.endsWith("/") || serverPath.endsWith("\\")) ? serverPath.substring(0, serverPath.length() - 1) //$NON-NLS-1$ //$NON-NLS-2$
                : serverPath;

        ArrayList<Item> toReturn = new ArrayList<Item>();

        for (String changesetItemPath : changesetData)
        {
            if (changesetItemPath.startsWith(serverPath))
            {
                Item item = new Item();
                item.setServerItem(changesetItemPath);
                item.setChangeSetID(changesetNumber);
                item.setItemType(ItemType.FILE);

                toReturn.add(item);
            }
        }

        Item[] items = new Item[toReturn.size()];
        return toReturn.toArray(items);
    }

    public Shelveset[] queryShelvesets(String shelvesetName, String shelvesetOwner)
    {
        return null;
    }

    public PendingSet[] queryShelvesetChanges(Shelveset shelveset, boolean includeDownloadInfo)
    {
        return null;
    }

    public void deleteShelveset(Shelveset shelveset)
    {

    }
}
