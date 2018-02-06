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

package com.microsoft.gittf.client.clc.commands.framework;

import com.microsoft.gittf.client.clc.Console;
import com.microsoft.gittf.client.clc.Console.Verbosity;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.core.OutputConstants;
import com.microsoft.gittf.core.util.shelveset.ShelvesetView;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkItemCheckedInfo;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.util.Check;

public class ShelvesetConsoleView
    implements ShelvesetView
{
    private final Console console;

    public ShelvesetConsoleView(final Console console)
    {
        Check.notNull(console, "console"); //$NON-NLS-1$

        this.console = console;
    }

    public void displayShelvesets(final Shelveset[] shelvesets, final boolean displayDetails)
    {
        displayHeader(shelvesets.length);

        if (!displayDetails)
        {
            displayTableHeader();
        }

        int count = 0;
        for (Shelveset shelveset : shelvesets)
        {
            displayShelveset(shelveset, displayDetails);

            if (displayDetails)
            {
                if (count != shelvesets.length - 1)
                    displayMessage(""); //$NON-NLS-1$
            }

            count++;
        }

        if (!displayDetails)
        {
            displayTableFooter();
        }
    }

    public void displayShelvesetDetails(Shelveset shelveset, PendingSet[] shelvesetDetails)
    {
        displayHeader(1);

        displayShelveset(shelveset, shelvesetDetails);
    }

    private void displayHeader(int shelvesetCount)
    {
        displayMessage(""); //$NON-NLS-1$

        displayMessage(Messages.formatString("ShelvesetConsoleView.HeaderFormat", shelvesetCount)); //$NON-NLS-1$

        displayMessage(""); //$NON-NLS-1$
    }

    private void displayTableHeader()
    {
        displayMessage(Messages.getString("ShelvesetConsoleView.TableHeader")); //$NON-NLS-1$
        displayMessage(Messages.getString("ShelvesetConsoleView.TableSeparator")); //$NON-NLS-1$
    }

    private void displayTableFooter()
    {
        displayMessage(Messages.getString("ShelvesetConsoleView.TableSeparator")); //$NON-NLS-1$
    }

    private void displayShelveset(Shelveset shelveset, boolean displayDetails)
    {
        if (displayDetails)
        {
            displayMessage(Messages.formatString("ShelvesetConsoleView.ShelvesetNameFormat", //$NON-NLS-1$
                shelveset.getName()));

            displayMessage(Messages.formatString("ShelvesetConsoleView.ShelvesetOwnerFormat", //$NON-NLS-1$
                shelveset.getOwnerDisplayName(),
                shelveset.getOwnerName()));

            displayWorkItemInfo(shelveset.getBriefWorkItemInfo());

            displayMessage(Messages.formatString("ShelvesetConsoleView.ShelvesetCommentFormat", //$NON-NLS-1$
                shelveset.getComment() == null ? OutputConstants.NEW_LINE : shelveset.getComment()));
        }
        else
        {
            displayMessage(Messages.formatString(
                "ShelvesetConsoleView.ShelvesetFormat", shelveset.getName(), shelveset.getOwnerName())); //$NON-NLS-1$
        }
    }

    private void displayWorkItemInfo(WorkItemCheckedInfo[] workItemsInfo)
    {
        String associatedWorkItems = ""; //$NON-NLS-1$
        String resolvedWorkItems = ""; //$NON-NLS-1$

        for (WorkItemCheckedInfo wi : workItemsInfo)
        {
            if (wi.getCheckinAction() == CheckinWorkItemAction.RESOLVE)
            {
                resolvedWorkItems += wi.getID() + " "; //$NON-NLS-1$
            }
            else
            {
                associatedWorkItems += wi.getID() + " "; //$NON-NLS-1$
            }
        }

        if (resolvedWorkItems.length() == 0)
        {
            resolvedWorkItems = Messages.getString("ShelvesetConsoleView.ShelvesetWorkItemNone"); //$NON-NLS-1$
        }

        if (associatedWorkItems.length() == 0)
        {
            associatedWorkItems = Messages.getString("ShelvesetConsoleView.ShelvesetWorkItemNone"); //$NON-NLS-1$
        }

        displayMessage(Messages.formatString("ShelvesetConsoleView.ShelvesetResolvedWorkItemsFormat", //$NON-NLS-1$
            resolvedWorkItems));

        displayMessage(Messages.formatString("ShelvesetConsoleView.ShelvesetAssociatedWorkItemsFormat", //$NON-NLS-1$
            associatedWorkItems));
    }

    private void displayShelveset(Shelveset shelveset, PendingSet[] shelvesetDetails)
    {
        displayShelveset(shelveset, true);

        displayMessage(Messages.getString("ShelvesetConsoleView.ChangesTableHeader")); //$NON-NLS-1$
        displayMessage(Messages.getString("ShelvesetConsoleView.TableSeparator")); //$NON-NLS-1$

        for (PendingSet pendingSet : shelvesetDetails)
        {
            for (PendingChange pendingChange : pendingSet.getPendingChanges())
            {
                displayMessage(Messages.formatString("ShelvesetConsoleView.PendingChangeFormat", //$NON-NLS-1$
                    pendingChange.getChangeType().toUIString(false),
                    pendingChange.getServerItem()));
            }
        }
    }

    private void displayMessage(String message)
    {
        if (console.getVerbosity() != Verbosity.QUIET)
        {
            console.getOutputStream().println(message);
        }
    }
}
