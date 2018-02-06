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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.microsoft.gittf.core.util.RepositoryPath;
import com.microsoft.tfs.util.Check;

/**
 * Represents a collection of changes in the git repository that can be pended
 * against the TFS server
 * 
 */
public class CheckinAnalysisChangeCollection
{
    private final Repository repository;
    private final RevObject targetTree;
    private final RevObject sourceTree;

    private final List<AddChange> adds = new ArrayList<AddChange>();
    private final List<EditChange> edits = new ArrayList<EditChange>();
    private final List<DeleteChange> deletes = new ArrayList<DeleteChange>();
    private final List<RenameChange> renames = new ArrayList<RenameChange>();
    private final List<PropertyChange> properties = new ArrayList<PropertyChange>();

    private boolean processDeletedFolders = true;

    private Set<String> processedDeletedFolders = new HashSet<String>();

    /**
     * Constructor
     */
    public CheckinAnalysisChangeCollection()
    {
        this.repository = null;
        this.sourceTree = null;
        this.targetTree = null;
    }

    /**
     * Constructor
     * 
     * @param repository
     *        the git repository
     * @param sourceTree
     *        the source tree in git repository used along with the target tree
     *        to determine folder deletes and folder renames
     * @param targetTree
     *        the target tree in git repository
     */
    public CheckinAnalysisChangeCollection(
        final Repository repository,
        final RevObject sourceTree,
        final RevObject targetTree)
    {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(sourceTree, "sourceTree"); //$NON-NLS-1$
        Check.notNull(targetTree, "targetTree"); //$NON-NLS-1$

        this.repository = repository;
        this.sourceTree = sourceTree;
        this.targetTree = targetTree;
    }

    /**
     * Sets whether the structure should identify deleted folders
     * 
     * @param processDeletedFolders
     *        - whether or not deleted folders should be detected
     */
    public void setProcessDeletedFolders(boolean processDeletedFolders)
    {
        this.processDeletedFolders = processDeletedFolders;
    }

    /**
     * Determines if the collection is empty
     * 
     * @return
     */
    public boolean isEmpty()
    {
        return adds.isEmpty() && edits.isEmpty() && deletes.isEmpty() && renames.isEmpty() && properties.isEmpty();
    }

    /**
     * Gets the list of add changes
     * 
     * @return
     */
    public final List<AddChange> getAdds()
    {
        return Collections.unmodifiableList(adds);
    }

    /**
     * Gets the list of edit changes
     * 
     * @return
     */
    public final List<EditChange> getEdits()
    {
        return Collections.unmodifiableList(edits);
    }

    /**
     * Gets the list of delete changes
     * 
     * @return
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws CorruptObjectException
     * @throws IOException
     */
    public final List<DeleteChange> getDeletes()
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        return Collections.unmodifiableList(deletes);
    }

    /**
     * Gets the list of rename changes
     * 
     * @return
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws CorruptObjectException
     * @throws IOException
     */
    public final List<RenameChange> getRenames()
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        return Collections.unmodifiableList(renames);
    }

    /**
     * Gets the list of property changes
     * 
     * @return
     */
    public final List<PropertyChange> getProperties()
    {
        return Collections.unmodifiableList(properties);
    }

    /**
     * Adds an add change
     * 
     * @param change
     * @throws Exception
     */
    public final void pendAdd(AddChange change)
        throws Exception
    {
        adds.add(change);
    }

    /**
     * Adds an edit change
     * 
     * @param change
     * @throws Exception
     */
    public final void pendEdit(EditChange change)
        throws Exception
    {
        edits.add(change);
    }

    /**
     * Adds a delete change
     * 
     * @param change
     * @throws Exception
     */
    public final void pendDelete(DeleteChange change)
        throws Exception
    {
        /* if the structure should identify deleted folders */
        if (processDeletedFolders)
        {
            /* check whether a delete needs to be pended for the folder or not */
            pendFolderDeleteIfNeeded(change.getType() == FileMode.TREE ? change.getPath()
                : RepositoryPath.getParent(change.getPath()));
        }

        deletes.add(change);
    }

    /**
     * Adds a rename change
     * 
     * @param change
     * @throws Exception
     */
    public final void pendRename(RenameChange change)
        throws Exception
    {
        renames.add(change);
    }

    /**
     * Adds a property change
     * 
     * @param change
     * @throws Exception
     */
    public final void pendProperty(PropertyChange change)
        throws Exception
    {
        properties.add(change);
    }

    /**
     * Adds a property change
     * 
     * @param change
     * @throws Exception
     */
    public final void pendPropertyIfChanged(PropertyChange change)
        throws Exception
    {
        if (change.isPropertyChanged())
        {
            properties.add(change);
        }
    }

    /**
     * Determies the size of the collection
     * 
     * @return
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws CorruptObjectException
     * @throws IOException
     */
    public int size()
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        return adds.size() + deletes.size() + edits.size() + renames.size() + properties.size();
    }

    /**
     * Creates a folder rename detector to detect folder renames in the change
     * list
     * 
     * @return
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws CorruptObjectException
     * @throws IOException
     */
    public TfsFolderRenameDetector createFolderRenameDetector()
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        if (repository == null || sourceTree == null || targetTree == null)
        {
            return new TfsFolderRenameDetector();
        }

        return new TfsFolderRenameDetector(repository, sourceTree, targetTree, getRenames());
    }

    /**
     * Pends a folder delete if the folder has used to exist in the source tree
     * and no longer exist in the target tree
     * 
     * @param folderPath
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws CorruptObjectException
     * @throws IOException
     */
    private void pendFolderDeleteIfNeeded(String folderPath)
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        /*
         * if the repository or the target tree are null the calculation cannot
         * be performed
         */
        if (repository == null || targetTree == null)
        {
            return;
        }

        ObjectReader objectReader = repository.newObjectReader();
        try
        {
            /*
             * if this folder is deleted we need to walk its parents recursively
             * until we find the upper most folder that was deleted
             */
            String folderToDelete = getUpperMostFolderToDelete(folderPath, null, objectReader, targetTree);

            /* if this folder was deleted already there is nothing else to do */
            if (processedDeletedFolders.contains(folderToDelete))
            {
                return;
            }

            /* pend a delete for the folder */
            processedDeletedFolders.add(folderToDelete);

            if (folderToDelete != null && folderToDelete.length() != 0)
            {
                deletes.add(new DeleteChange(folderToDelete, FileMode.TREE));
            }
        }
        finally
        {
            if (objectReader != null)
            {
                objectReader.release();
            }
        }
    }

    private String getUpperMostFolderToDelete(
        String filePath,
        String previousFilePath,
        ObjectReader objectReader,
        RevObject commitRevTree)
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        /*
         * if the file path is empty then we reached the end of the recursive
         * walk
         */
        if (filePath == null || filePath.length() == 0)
        {
            return previousFilePath;
        }

        /*
         * if this folder does not exist in the tree then we need to check its
         * parent
         */
        if (!folderExistsInTree(filePath, objectReader, commitRevTree))
        {
            return getUpperMostFolderToDelete(RepositoryPath.getParent(filePath), filePath, objectReader, commitRevTree);
        }

        /* otherwise the previous file path was the one deleted */
        return previousFilePath;
    }

    /**
     * Determines if the folder path exists in the tree
     * 
     * @param filePath
     *        the folder path
     * @param objectReader
     *        the object reader
     * @param commitRevTree
     *        the tree to check
     * @return
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws CorruptObjectException
     * @throws IOException
     */
    private boolean folderExistsInTree(String filePath, ObjectReader objectReader, RevObject commitRevTree)
        throws MissingObjectException,
            IncorrectObjectTypeException,
            CorruptObjectException,
            IOException
    {
        TreeWalk folder = null;
        try
        {
            /* Check the tree walk object for the folder */
            folder = TreeWalk.forPath(objectReader, filePath, commitRevTree);

            /* if the folder exists the treewalk object will not be null */
            return folder != null;
        }
        finally
        {
            if (folder != null)
            {
                folder.release();
            }
        }
    }
}
