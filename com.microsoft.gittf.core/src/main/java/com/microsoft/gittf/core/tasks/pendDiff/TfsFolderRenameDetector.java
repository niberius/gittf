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

package com.microsoft.gittf.core.tasks.pendDiff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.microsoft.gittf.core.util.RepositoryPath;
import com.microsoft.tfs.util.Check;

/**
 * TFsFolderRenameDetector detects the possible folder rename combinations from
 * a list of file rename combinations. Git does not track folder renames, it
 * only tracks file renames. This is problematic when pending these renames in
 * TFS, for example if we had these files Folder\a and Folder\b then we renamed
 * Folder to Folder-rename. In git the list of changes will be Folder\a ->
 * Folder-rename\a and Folder\b -> Folder-rename\b. If these two changes were
 * pended in TFS a Folder\ will still exist in TFS and will be stale and empty.
 * Folder\ could not be deleted too because TFS does not support deleting an
 * empty folder which is the source of a rename.
 * 
 * The purpose of this class is to process a list of file renames and try to
 * figure out a possible combination of folder renames as accurately as
 * possible.
 * 
 */
public class TfsFolderRenameDetector
{
    private final Repository repository;
    private final RevObject targetTree;
    private final RevObject sourceTree;
    private final List<RenameChange> fileRenames;

    private Set<RenameChange> sortedFileRenames = new TreeSet<RenameChange>(new RenameChangeOldPathCompartor());
    private Map<String, RenameChange> processedRenames = new HashMap<String, RenameChange>();
    private Set<String> processedOldPaths = new TreeSet<String>(Collections.reverseOrder());

    private List<RenameChange> resultRenames = new ArrayList<RenameChange>();
    private List<List<RenameChange>> resultBatchedRenames = new ArrayList<List<RenameChange>>();

    /**
     * Constructor - creates an empty detector that does nothing
     * 
     */
    public TfsFolderRenameDetector()
    {
        this.repository = null;
        this.sourceTree = null;
        this.targetTree = null;
        this.fileRenames = new ArrayList<RenameChange>();
    }

    /**
     * Constructor
     * 
     * @param repository
     *        the git repository
     * @param sourceTree
     *        the source tree
     * @param targetTree
     *        the target tree
     * @param fileRenames
     *        the list of files that were renamed
     */
    public TfsFolderRenameDetector(
        Repository repository,
        RevObject sourceTree,
        RevObject targetTree,
        List<RenameChange> fileRenames)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(sourceTree, "sourceTree"); //$NON-NLS-1$
        Check.notNull(targetTree, "targetTree"); //$NON-NLS-1$
        Check.notNull(fileRenames, "fileRenames"); //$NON-NLS-1$

        this.repository = repository;
        this.sourceTree = sourceTree;
        this.targetTree = targetTree;
        this.fileRenames = fileRenames;
    }

    /**
     * Return the list of renames in batches. Which batch a rename belongs too
     * is determined by the depth of the original path.This will be empty until
     * compute() has been called.
     * 
     * @return
     */
    public List<List<RenameChange>> getRenameBatches()
    {
        return resultBatchedRenames;
    }

    /**
     * Returns all the renames. This will be empty until compute() has been
     * called.
     * 
     * @return
     */
    public List<RenameChange> getRenames()
    {
        return resultRenames;
    }

    /**
     * Computes the folder renames from the list of file renames
     * 
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws CorruptObjectException
     * @throws IOException
     */
    public void compute()
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        /* Sort the file renames to process them in the correct sequence */
        buildSortedFileRenames();

        for (RenameChange rename : sortedFileRenames)
        {
            /*
             * If this is a file only rename e.g. Folder\SubFolder\a.txt ->
             * Folder\SubFolder\a-r.txt OR if this is descendant destination
             * rename e.g. Folder\SubFolder\a.txt ->
             * Folder\SubFolder\NewFolder\a.txt then just add the rename as is
             * to the list since there is no folder rename involved.
             */
            if (isFileOnlyRename(rename) || isDecendantDestinationRename(rename))
            {
                addRenameToResult(rename.getOldPath(), rename);
            }
            /* Otherwise process the file rename */
            else
            {
                processRename(rename);
            }
        }
    }

    /**
     * Sorts the renames using their old path
     */
    private void buildSortedFileRenames()
    {
        for (RenameChange rename : fileRenames)
        {
            sortedFileRenames.add(rename);
        }
    }

    /**
     * Process a rename
     * 
     * @param rename
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws CorruptObjectException
     * @throws IOException
     */
    private void processRename(RenameChange rename)
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        /*
         * Renames are being processed level by level where each level
         * designates a depth in the path. Renames are processed by comparing
         * the most deep level then comparing the depth - 1 level etc.
         */
        processRenameLevel(rename.getOldPath(), rename.getNewPath());

        /*
         * Make sure that the rename change is accounted for, by ensuring that
         * the rename is processing have generated a possible operation that
         * performs the rename operation, also ensure that if this rename change
         * has the edit flag, the edit is pended
         */
        ensureRenameAccountedFor(rename);
    }

    /**
     * To determine the folder rename operation the old path and the new path
     * are aligned where the deepest level in each path e.g. if
     * Folder\SubFolder\a.txt was renamed to Folder-r\SubFolder-r\a-r.txt the
     * paths are aligned as follows
     * 
     * Folder\ SubFolder\ a.txt Folder-r\SubFolder-r\a-r.txt
     * 
     * Where a.txt and a-r.txt are tested first for rename, then SubFolder and
     * SubFolder-r are tested for rename etc.
     * 
     * By walking the path backwards we terminate quickly once we find the
     * folder in the new path that existed in the old tree and thus was not part
     * of the rename
     * 
     * @param oldPath
     * @param newPath
     * @return
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws CorruptObjectException
     * @throws IOException
     */
    private boolean processRenameLevel(String oldPath, String newPath)
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        /*
         * If any of the paths are empty then we reached the end of the
         * recursion target
         */
        if (oldPath == null || oldPath.length() == 0 || newPath == null || newPath.length() == 0)
        {
            return false;
        }

        /* We need to update the oldPath with any parent renames first */
        String oldPathToUse = updateOldPathWithProcessed(oldPath, newPath);
        String newPathToUse = newPath;

        /* if the old and new paths are equal we are done here */
        if (oldPath.equals(newPath))
        {
            return false;
        }

        /*
         * if the newPath is an ancestor for the old Path we are done here we
         * just need to make sure that the layer above this one pends a rename
         */
        if (RepositoryPath.isAncestor(newPathToUse, oldPathToUse))
        {
            return true;
        }

        /* Should we rename this layer ? */
        boolean addThisLayer = false;
        String oldFileName = RepositoryPath.getFileName(oldPathToUse);
        String newFileName = RepositoryPath.getFileName(newPathToUse);

        /*
         * if this layer old file name is not equal to the new file name, then
         * yes
         */
        if (!oldFileName.equals(newFileName))
        {
            addThisLayer = true;
        }

        /*
         * if we recieved a notification that we need to rename this layer, then
         * yes
         */
        if (processRenameLevel(RepositoryPath.getParent(oldPath), RepositoryPath.getParent(newPathToUse)))
        {
            addThisLayer = true;
        }

        /* If we need to add a rename for this layer then do so here */
        if (addThisLayer)
        {
            /* Determine the source and destination of the rename operation */
            oldPathToUse = updateOldPathWithProcessed(oldPath, newPath);
            newPathToUse = newPath;

            /* If this path has already been renamed then ignore */
            if (processedOldPaths.contains(oldPath))
            {
                return false;
            }

            /*
             * If the new folder path did not exist in the source tree and the
             * old folder path does not exist in the target tree then this
             * folder can be renamed
             */
            if (!folderExistsInTree(newPathToUse, sourceTree) && !folderExistsInTree(oldPathToUse, targetTree))
            {
                addRenameToResult(oldPath, oldPathToUse, newPathToUse);

                return false;
            }
        }

        return false;
    }

    /**
     * Ensures that the rename change specified is accounted for by the rename
     * operation we have so far
     * 
     * @param rename
     *        the rename chage
     */
    private void ensureRenameAccountedFor(RenameChange rename)
    {
        /* Update the old paht with processed information */
        String updatedOldPath = updateOldPathWithProcessed(rename.getOldPath(), rename.getNewPath());

        /*
         * If the updated old path is equal to the new path then yes it is
         * accounted for
         */
        if (!updatedOldPath.equals(rename.getNewPath()))
        {
            addRenameToResult(
                rename.getOldPath(),
                new RenameChange(updatedOldPath, rename.getNewPath(), rename.getObjectID(), rename.isEdit()));
        }
        /*
         * Other wise we just need to make sure that the edit is accounted for
         * if needed
         */
        else
        {
            if (rename.isEdit())
            {
                ensureEditAccountedFor(rename);
            }
        }
    }

    /**
     * Ensures that the rename change edit flag is accounted for
     * 
     * @param rename
     */
    private void ensureEditAccountedFor(RenameChange rename)
    {
        if (processedOldPaths.contains(rename.getOldPath()))
        {
            processedRenames.get(rename.getOldPath()).updateEditInformation(rename.getObjectID());
        }
        else
        {
            String updatedOldPath = updateOldPathWithProcessed(rename.getOldPath(), rename.getNewPath());
            addRenameToResult(
                rename.getOldPath(),
                new RenameChange(updatedOldPath, rename.getNewPath(), rename.getObjectID(), rename.isEdit()));
        }
    }

    /**
     * Add the rename to the result
     * 
     * @param unprocessedOldPath
     *        the original old path without any processing
     * @param oldPath
     *        the old path to use as the source of the rename operation
     * @param newPath
     *        the new path to use as the destination of the rename operation
     */
    private void addRenameToResult(String unprocessedOldPath, String oldPath, String newPath)
    {
        addRenameToResult(unprocessedOldPath, new RenameChange(oldPath, newPath, ObjectId.zeroId(), false));
    }

    /**
     * Add the rename change to the result
     * 
     * @param unprocessedOldPath
     *        the old path without any processing
     * @param rename
     *        the rename change to add to results
     */
    private void addRenameToResult(String unprocessedOldPath, RenameChange rename)
    {
        if (processedOldPaths.contains(rename.getOldPath()))
        {
            RenameChange addedRenameObject = processedRenames.get(rename.getOldPath());
            if (addedRenameObject.isEdit() != rename.isEdit())
            {
                addedRenameObject.updateEditInformation(rename.getObjectID());
            }

            return;
        }

        resultRenames.add(rename);
        processedOldPaths.add(unprocessedOldPath);
        processedRenames.put(unprocessedOldPath, rename);

        addRenameToBatchedResult(rename);
    }

    /**
     * Add rename to batched results
     * 
     * @param rename
     *        the rename change to add
     */
    private void addRenameToBatchedResult(RenameChange rename)
    {
        int depth = RepositoryPath.getFolderDepth(rename.getNewPath());

        for (int toAdd = resultBatchedRenames.size(); toAdd <= depth; toAdd++)
        {
            resultBatchedRenames.add(new ArrayList<RenameChange>());
        }

        resultBatchedRenames.get(depth).add(rename);
    }

    /**
     * Returns true if the rename change designates a file (leaf) rename
     * 
     * @param rename
     *        the rename change
     * @return
     */
    private boolean isFileOnlyRename(RenameChange rename)
    {
        String oldParent = RepositoryPath.getParent(rename.getOldPath());
        String newParent = RepositoryPath.getParent(rename.getNewPath());

        return oldParent.equals(newParent);
    }

    /**
     * Returns true if the rename change designates a file that was renamed to a
     * sub folder
     * 
     * @param rename
     *        the rename change
     * @return
     */
    private boolean isDecendantDestinationRename(RenameChange rename)
    {
        String oldParent = RepositoryPath.getParent(rename.getOldPath());
        String newParent = RepositoryPath.getParent(rename.getNewPath());

        return RepositoryPath.isAncestor(newParent, oldParent);
    }

    /**
     * Updates the old path specified to the new current destination using the
     * processed path list so far
     * 
     * @param oldPath
     *        the old path to process
     * @param newPath
     *        the expected new path
     * @return
     */
    private String updateOldPathWithProcessed(String oldPath, String newPath)
    {
        /* Loop over all the processed renames so far */
        for (String processedPath : processedOldPaths)
        {
            /*
             * if the processed rename is a parent of the old path then update
             * the old path only if the new Path match the processed data
             */
            if (RepositoryPath.isAncestor(oldPath, processedPath))
            {
                RenameChange processedRename = processedRenames.get(processedPath);

                if (RepositoryPath.isAncestor(newPath, processedRename.getNewPath()))
                {
                    return processedRename.getNewPath() + oldPath.substring(processedPath.length());
                }
            }

            /*
             * if the processed rename is equal to the old path then update the
             * old path only if the new Path match the processed data
             */
            if (oldPath.equals(processedPath))
            {
                RenameChange processedRename = processedRenames.get(processedPath);

                if (newPath.equals(processedRename.getNewPath()))
                {
                    return processedRename.getNewPath();
                }
            }
        }

        return oldPath;
    }

    /**
     * Returns true if the folder path specifies exist in the tree
     * 
     * @param filePath
     * @param commitRevTree
     * @return
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws CorruptObjectException
     * @throws IOException
     */
    private boolean folderExistsInTree(String filePath, RevObject commitRevTree)
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        ObjectReader objectReader = repository.newObjectReader();
        TreeWalk folder = null;
        try
        {
            folder = TreeWalk.forPath(objectReader, filePath, commitRevTree);
            return folder != null;
        }
        finally
        {
            if (folder != null)
            {
                folder.release();
            }

            if (objectReader != null)
            {
                objectReader.release();
            }
        }
    }
}
