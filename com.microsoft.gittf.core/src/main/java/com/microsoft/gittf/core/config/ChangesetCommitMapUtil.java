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

package com.microsoft.gittf.core.config;

import org.eclipse.jgit.lib.ObjectId;

import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;

public final class ChangesetCommitMapUtil
{
    private ChangesetCommitMapUtil()
    {
    }

    public static ChangesetCommitDetails getLastBridgedChangeset(ChangesetCommitMap commitMap)
        throws Exception
    {
        Check.notNull(commitMap, "commitMap"); //$NON-NLS-1$

        int lastChangesetID = commitMap.getLastBridgedChangesetID(true);

        if (lastChangesetID < 0)
        {
            return null;
        }

        ObjectId lastCommitID = commitMap.getCommitID(lastChangesetID, true);

        if (lastCommitID == null)
        {
            return null;
        }

        return new ChangesetCommitDetails(lastChangesetID, lastCommitID);
    }

    public static ChangesetCommitDetails getLatestChangeset(
        ChangesetCommitMap commitMap,
        VersionControlClient versionControlClient,
        final String serverPath)
    {
        Check.notNull(commitMap, "commitMap"); //$NON-NLS-1$
        Check.notNull(versionControlClient, "versionControlClient"); //$NON-NLS-1$
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$
        Check.isTrue(ServerPath.isServerPath(serverPath), "serverPath.isServerPath"); //$NON-NLS-1$

        /*
         * Query the last changeset on the server to determine if the
         * destination path exists and its current changeset.
         */
        Changeset[] changesets =
            versionControlClient.queryHistory(
                serverPath,
                LatestVersionSpec.INSTANCE,
                0,
                RecursionType.FULL,
                null,
                new ChangesetVersionSpec(0),
                LatestVersionSpec.INSTANCE,
                1,
                false,
                false,
                false,
                false);

        if (changesets.length == 1)
        {
            final int latestChangesetID = changesets[0].getChangesetID();
            final ObjectId latestCommitID = commitMap.getCommitID(latestChangesetID, true);

            return new ChangesetCommitDetails(latestChangesetID, latestCommitID);
        }
        else
        {
            return null;
        }
    }

    public static final class ChangesetCommitDetails
    {
        private final int changesetID;
        private final ObjectId commitID;

        public ChangesetCommitDetails(final int changesetID, final ObjectId commitID)
        {
            this.changesetID = changesetID;
            this.commitID = commitID;
        }

        public final int getChangesetID()
        {
            return changesetID;
        }

        public final ObjectId getCommitID()
        {
            return commitID;
        }
    }
}
