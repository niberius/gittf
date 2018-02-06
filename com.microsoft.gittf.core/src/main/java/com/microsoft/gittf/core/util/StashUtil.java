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
import java.text.MessageFormat;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public final class StashUtil
{
    private static String STASH_INDEX_COMMENT = "index on {0}: {1} {2}"; //$NON-NLS-1$
    private static String STASH_COMMENT = "WIP on {0}: {1} {2}"; //$NON-NLS-1$

    private StashUtil()
    {
    }

    /**
     * Creates a stash
     * 
     * @param repository
     *        the git repository
     * @param repositoryInserter
     *        the repository inserter object to use
     * @param rootBaseTree
     *        the tree id for the base commit of the stash
     * @param rootStashTree
     *        the tree id for the commit of the stash
     * @param rootIndexTree
     *        the tree id for the index commit of the stash
     * @param baseParentId
     *        the parent of the base tree commit
     * @param ownerDisplayName
     *        the owner display name of the stash
     * @param ownerName
     *        the owner name of the stash
     * @param stashComment
     *        the comment used to for the stash
     * @param stashName
     *        the stash name
     * @return
     * @throws IOException
     */
    public static final ObjectId create(
        final Repository repository,
        final ObjectInserter repositoryInserter,
        final ObjectId rootBaseTree,
        final ObjectId rootStashTree,
        final ObjectId rootIndexTree,
        final ObjectId baseParentId,
        final String ownerDisplayName,
        final String ownerName,
        final String stashComment,
        final String stashName)
        throws IOException
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(repositoryInserter, "repositoryInserter"); //$NON-NLS-1$
        Check.notNull(rootBaseTree, "rootBaseTree"); //$NON-NLS-1$
        Check.notNull(rootStashTree, "rootStashTree"); //$NON-NLS-1$
        Check.notNull(rootIndexTree, "rootIndexTree"); //$NON-NLS-1$

        /* identifies the head and the branch we are creating the stash for */
        Ref headReference = repository.getRef(Constants.HEAD);
        RevCommit headCommit = new RevWalk(repository).parseCommit(headReference.getObjectId());
        String currentBranchName = Repository.shortenRefName(headReference.getTarget().getName());

        PersonIdent author = new PersonIdent(ownerDisplayName, ownerName);

        /* create the base commit */
        CommitBuilder commitBuilder = new CommitBuilder();
        commitBuilder.setTreeId(rootBaseTree);
        if (baseParentId != null)
        {
            commitBuilder.setParentId(baseParentId);
        }
        commitBuilder.setMessage(stashComment);
        commitBuilder.setAuthor(author);
        commitBuilder.setCommitter(author);

        ObjectId baseCommit = repositoryInserter.insert(commitBuilder);

        /* create the index commit */
        commitBuilder.setTreeId(rootIndexTree);
        commitBuilder.setParentId(baseCommit);
        commitBuilder.setMessage(MessageFormat.format(
            STASH_INDEX_COMMENT,
            currentBranchName,
            headCommit.abbreviate(7).name(),
            stashName));
        commitBuilder.setAuthor(author);
        commitBuilder.setCommitter(author);

        ObjectId indexCommit = repositoryInserter.insert(commitBuilder);

        /* create the stash commit */
        commitBuilder.setTreeId(rootStashTree);
        commitBuilder.setParentId(baseCommit);
        commitBuilder.addParentId(indexCommit);

        String stashRefLogComment =
            MessageFormat.format(STASH_COMMENT, currentBranchName, headCommit.abbreviate(7).name(), stashName);
        commitBuilder.setMessage(stashRefLogComment);

        ObjectId stashCommit = repositoryInserter.insert(commitBuilder);

        repositoryInserter.flush();

        /* Update the stash reference and ref log */
        RefUpdate stashReferenceUpdate = repository.updateRef(Constants.R_STASH);
        stashReferenceUpdate.setNewObjectId(stashCommit);
        stashReferenceUpdate.setRefLogIdent(author);
        stashReferenceUpdate.setRefLogMessage(stashRefLogComment, false);

        Ref currentStashRef = repository.getRef(Constants.R_STASH);
        if (currentStashRef != null)
        {
            stashReferenceUpdate.setExpectedOldObjectId(currentStashRef.getObjectId());
        }
        else
        {
            stashReferenceUpdate.setExpectedOldObjectId(ObjectId.zeroId());
        }

        stashReferenceUpdate.forceUpdate();

        return stashCommit;
    }

    /**
     * Applys the stash to the current HEAD
     * 
     * @param repository
     *        the git repository
     * @param stashCommitId
     *        the commit it pointing to the stash
     * @throws WrongRepositoryStateException
     * @throws GitAPIException
     */
    public static final void apply(final Repository repository, final ObjectId stashCommitId)
        throws WrongRepositoryStateException,
            GitAPIException
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(stashCommitId, "stashCommitId"); //$NON-NLS-1$

        /*
         * TODO : Need to add apply code here that behaves similarly to the git
         * tools
         */
    }
}
