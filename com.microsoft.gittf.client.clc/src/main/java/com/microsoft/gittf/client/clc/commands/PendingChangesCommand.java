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

package com.microsoft.gittf.client.clc.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.arguments.Argument;
import com.microsoft.gittf.client.clc.arguments.ValueArgument;
import com.microsoft.gittf.client.clc.commands.framework.Command;
import com.microsoft.gittf.core.tasks.pendDiff.RenameMode;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.WorkItem;

public abstract class PendingChangesCommand
    extends Command
{
    private List<WorkItemCheckinInfo> workItemsCheckinInfo = null;

    protected RenameMode getRenameModeIfSpecified()
        throws Exception
    {
        String renameModeString = getArguments().contains("renamemode") ? //$NON-NLS-1$
            ((ValueArgument) getArguments().getArgument("renamemode")).getValue() : null; //$NON-NLS-1$

        if (renameModeString == null)
        {
            return RenameMode.JUSTFILES;
        }

        try
        {
            return RenameMode.valueOf(renameModeString.toUpperCase());
        }
        catch (Exception e)
        {
            throw new Exception(
                Messages.formatString("PendingChangesCommand.InvalidRenameModeFormat", renameModeString)); //$NON-NLS-1$
        }
    }

    protected WorkItemCheckinInfo[] getWorkItemCheckinInfo()
        throws Exception
    {
        if (workItemsCheckinInfo == null)
        {
            workItemsCheckinInfo = new ArrayList<WorkItemCheckinInfo>();
            Set<Integer> workItemsProcessedSoFar = new HashSet<Integer>();

            for (Argument resolveArgument : getArguments().getArguments("resolve")) //$NON-NLS-1$
            {
                WorkItem wi = getWorkItem((ValueArgument) resolveArgument);

                if (workItemsProcessedSoFar.contains(wi.getID()))
                {
                    throw new Exception(Messages.formatString(
                        "PendingChangesCommand.WorkItemSpecifiedMultipleTimesFormat", wi.getID())); //$NON-NLS-1$
                }

                workItemsProcessedSoFar.add(wi.getID());
                workItemsCheckinInfo.add(new WorkItemCheckinInfo(wi, CheckinWorkItemAction.RESOLVE));
            }

            for (Argument associateArgument : getArguments().getArguments("associate")) //$NON-NLS-1$
            {
                WorkItem wi = getWorkItem((ValueArgument) associateArgument);

                if (workItemsProcessedSoFar.contains(wi.getID()))
                {
                    throw new Exception(Messages.formatString(
                        "PendingChangesCommand.WorkItemSpecifiedMultipleTimesFormat", wi.getID())); //$NON-NLS-1$
                }

                workItemsProcessedSoFar.add(wi.getID());
                workItemsCheckinInfo.add(new WorkItemCheckinInfo(wi, CheckinWorkItemAction.ASSOCIATE));
            }
        }

        return workItemsCheckinInfo.toArray(new WorkItemCheckinInfo[workItemsCheckinInfo.size()]);
    }

    private WorkItem getWorkItem(ValueArgument argument)
        throws Exception
    {
        int id;

        try
        {
            id = Integer.parseInt(argument.getValue());

            if (id <= 0)
            {
                throw new Exception(Messages.formatString(
                    "PendingChangesCommand.WorkItemInvalidFormat", argument.getValue())); //$NON-NLS-1$
            }
        }
        catch (NumberFormatException e)
        {
            throw new Exception(Messages.formatString(
                "PendingChangesCommand.WorkItemInvalidFormat", argument.getValue())); //$NON-NLS-1$
        }

        WorkItem workItem = getConnection().getWorkItemClient().getWorkItemByID(id);

        if (workItem == null)
        {
            throw new Exception(Messages.formatString(
                "PendingChangesCommand.WorkItemDoesNotExistFormat", argument.getValue())); //$NON-NLS-1$
        }

        return workItem;
    }
}
