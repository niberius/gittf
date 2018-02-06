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

package com.microsoft.gittf.core.util;

import java.io.IOException;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RemoteRefUpdate;

import com.microsoft.gittf.core.GitTFConstants;

/**
 * 
 * Provides utility to allow clone, fetch and checkin commands to create and
 * update a branch / remote branch to point to what tfs contains. If the repo is
 * a bare repo we create both a remote reference and a branch. If the repo is
 * non-bare we only create a remote reference
 * 
 */
public final class TfsBranchUtil
{
    private TfsBranchUtil()
    {
    }

    /**
     * Creates a remote tracking ref for tfs. If the repo is bare a regular
     * branch is created too.
     * 
     * @param repository
     * @throws RefAlreadyExistsException
     * @throws RefNotFoundException
     * @throws InvalidRefNameException
     * @throws GitAPIException
     * @throws IOException
     */
    public static void create(Repository repository)
        throws RefAlreadyExistsException,
            RefNotFoundException,
            InvalidRefNameException,
            GitAPIException,
            IOException
    {
        create(repository, null);
    }

    /**
     * 
     * Creates a remote tracking ref for tfs. If the repo is bare a regular
     * branch is created too.
     * 
     * @param repository
     * @param startPoint
     * @throws RefAlreadyExistsException
     * @throws RefNotFoundException
     * @throws InvalidRefNameException
     * @throws GitAPIException
     * @throws IOException
     */
    public static void create(Repository repository, String startPoint)
        throws RefAlreadyExistsException,
            RefNotFoundException,
            InvalidRefNameException,
            GitAPIException,
            IOException
    {
        if (repository.isBare())
        {
            CreateBranchCommand createBranch = new Git(repository).branchCreate();
            createBranch.setName(GitTFConstants.GIT_TF_BRANCHNAME);
            createBranch.setForce(true);
            if (startPoint != null && startPoint.length() > 0)
            {
                createBranch.setStartPoint(startPoint);
            }
            createBranch.call();
        }

        TfsRemoteReferenceUpdate remoteRefUpdate = new TfsRemoteReferenceUpdate(repository, startPoint);
        remoteRefUpdate.update();
    }

    /**
     * Updates the remote tracking branch and branch to point at the commit
     * specified.
     * 
     * @param repository
     * @param commitId
     * @throws IOException
     * @throws RefAlreadyExistsException
     * @throws RefNotFoundException
     * @throws InvalidRefNameException
     * @throws GitAPIException
     */
    public static void update(Repository repository, ObjectId commitId)
        throws IOException,
            RefAlreadyExistsException,
            RefNotFoundException,
            InvalidRefNameException,
            GitAPIException
    {
        if (repository.isBare())
        {
            Ref tfsBranchRef = repository.getRef(Constants.R_HEADS + GitTFConstants.GIT_TF_BRANCHNAME);
            if (tfsBranchRef == null)
            {
                create(repository);
            }

            RefUpdate ref = repository.updateRef(Constants.R_HEADS + GitTFConstants.GIT_TF_BRANCHNAME);
            ref.setNewObjectId(commitId);
            ref.setForceUpdate(true);
            ref.update();
        }

        TfsRemoteReferenceUpdate remoteRefUpdate = new TfsRemoteReferenceUpdate(repository, commitId.name());
        remoteRefUpdate.update();

    }

    private static class TfsRemoteReferenceUpdate
        extends RemoteRefUpdate
    {
        private final Repository repository;

        public TfsRemoteReferenceUpdate(Repository repository, String referenceName)
            throws IOException
        {
            super(
                repository,
                referenceName,
                "", true, Constants.R_REMOTES + GitTFConstants.GIT_TF_REMOTE + GitTFConstants.GIT_TF_BRANCHNAME, null); //$NON-NLS-1$

            this.repository = repository;
        }

        public void update()
            throws IOException
        {
            RevWalk revWalk = new RevWalk(repository);
            try
            {
                updateTrackingRef(revWalk);
            }
            finally
            {
                if (revWalk != null)
                {
                    revWalk.release();
                }
            }
        }
    }
}
