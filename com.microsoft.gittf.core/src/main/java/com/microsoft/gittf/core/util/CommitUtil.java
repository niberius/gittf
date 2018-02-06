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
import java.util.Collection;

import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import com.microsoft.gittf.core.Messages;

public final class CommitUtil
{
    private CommitUtil()
    {
    }

    /**
     * Determines if the commit id specified is a valid commit in the repository
     * 
     * @param repository
     *        the git repository
     * @param objectId
     *        the commit object id
     * @return
     */
    public static boolean isValidCommitId(final Repository repository, ObjectId objectId)
    {
        final RevWalk walker = new RevWalk(repository);

        try
        {
            RevCommit commit = walker.parseCommit(objectId);

            return commit != null;
        }
        catch (Exception exception)
        {
            return false;
        }
        finally
        {
            if (walker != null)
            {
                walker.release();
            }
        }
    }

    /**
     * Returns the commit object id that is pointed at by the ref specified
     * 
     * @param repository
     *        the git repository
     * @param ref
     *        the reference name
     * @return
     * @throws Exception
     */
    public static ObjectId getRefNameCommitID(final Repository repository, String ref)
        throws Exception
    {
        if (AbbreviatedObjectId.isId(ref) || ObjectId.isId(ref))
        {
            return peelRef(repository, repository.resolve(ref));
        }

        return getCommitId(repository, ref);
    }

    /**
     * Returns the commit id referenced by HEAD
     * 
     * @param repository
     *        the git repository
     * @return
     * @throws Exception
     */
    public static ObjectId getCurrentBranchHeadCommitID(final Repository repository)
        throws Exception
    {
        return getCommitId(repository, Constants.HEAD);
    }

    /**
     * Returns the commit id referenced by refs/heads/master
     * 
     * @param repository
     *        the git repository
     * @return
     * @throws Exception
     */
    public static ObjectId getMasterHeadCommitID(final Repository repository)
        throws Exception
    {
        return getCommitId(repository, Constants.R_HEADS + Constants.MASTER);
    }

    private static ObjectId getCommitId(final Repository repository, String refName)
        throws Exception
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        Ref ref = repository.getRef(refName);

        if (ref == null)
        {
            throw new Exception(Messages.formatString("RepositoryUtil.NoRefFormat", refName)); //$NON-NLS-1$
        }

        ObjectId commitId = peelRef(repository, ref.getObjectId());

        if (commitId == null)
        {
            throw new Exception(Messages.formatString("RepositoryUtil.NoObjectForRefFormat", refName)); //$NON-NLS-1$
        }

        return commitId;

    }

    private static ObjectId peelRef(final Repository repository, ObjectId refId)
        throws MissingObjectException,
            IOException
    {
        RevWalk walker = new RevWalk(repository);
        try
        {
            return walker.peel(walker.parseAny(refId));
        }
        finally
        {
            if (walker != null)
            {
                walker.release();
            }
        }
    }

    /**
     * Resolves the abbreviated id specified
     * 
     * @param repository
     *        the git repository
     * @param objectID
     *        objectid to expand
     * @return
     */
    public static final ObjectId resolveAbbreviatedId(final Repository repository, final AbbreviatedObjectId objectID)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(objectID, "objectID"); //$NON-NLS-1$

        if (repository != null)
        {
            ObjectReader objReader = repository.getObjectDatabase().newReader();

            try
            {
                Collection<ObjectId> objects = objReader.resolve(objectID);

                if (objects.size() == 0)
                {
                    return null;
                }
                else if (objects.size() == 1)
                {
                    return objects.iterator().next();
                }
                else
                {
                    throw new RuntimeException(Messages.formatString("RepositoryUtil.AmbiguousObjectFormat", objectID)); //$NON-NLS-1$
                }
            }
            catch (IOException exception)
            {
                throw new RuntimeException(exception);
            }
            finally
            {
                if (objReader != null)
                {
                    objReader.release();
                }
            }
        }

        return null;
    }
}
