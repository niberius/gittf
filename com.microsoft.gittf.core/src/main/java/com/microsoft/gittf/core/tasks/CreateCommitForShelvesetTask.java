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

package com.microsoft.gittf.core.tasks;

import java.util.Calendar;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import com.microsoft.gittf.core.Messages;
import com.microsoft.gittf.core.interfaces.VersionControlService;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;

public class CreateCommitForShelvesetTask
    extends CreateCommitForPendingSetsTask
{
    private final Shelveset shelveset;

    public CreateCommitForShelvesetTask(
        final Repository repository,
        final VersionControlService versionControlClient,
        Shelveset shelveset,
        ObjectId parentCommitID)
    {
        super(repository, versionControlClient, parentCommitID);

        Check.notNull(shelveset, "shelveset"); //$NON-NLS-1$

        this.shelveset = shelveset;

        setCreateStashCommit(true);
    }

    @Override
    public String getProgressMonitorMessage()
    {
        return Messages.formatString("CreateCommitForShelvesetTask.UnshelvingShelvesetFormat", shelveset.getName()); //$NON-NLS-1$
    }

    @Override
    public PendingSet[] getPendingSets()
    {
        return versionControlService.queryShelvesetChanges(shelveset, true);
    }

    @Override
    public String getOwnerDisplayName()
    {
        return shelveset.getOwnerDisplayName();
    }

    @Override
    public String getOwner()
    {
        return shelveset.getOwnerName();
    }

    @Override
    public String getCommitterDisplayName()
    {
        return shelveset.getOwnerDisplayName();
    }

    @Override
    public String getCommitter()
    {
        return shelveset.getOwnerName();
    }

    @Override
    public Calendar getCommitDate()
    {
        return shelveset.getCreationDate();
    }

    @Override
    public String getComment()
    {
        return shelveset.getComment();
    }

    @Override
    public String getName()
    {
        return Messages.formatString("CreateCommitForShelvesetTask.ShelvesetNameFormat", shelveset.getName()); //$NON-NLS-1$
    }

}
