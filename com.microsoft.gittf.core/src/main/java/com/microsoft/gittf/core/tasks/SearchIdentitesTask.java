package com.microsoft.gittf.core.tasks;

import com.microsoft.gittf.core.interfaces.IdentityManagementService;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.tfs.core.clients.webservices.IdentitySearchFactor;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;

public class SearchIdentitesTask
    extends Task
{
    final IdentityManagementService IMS;
    final IdentitySearchFactor searchFactor;
    final String[] searchValues;

    private TeamFoundationIdentity[][] identities;

    public SearchIdentitesTask(IdentityManagementService IMS, IdentitySearchFactor searchFactor, String[] searchValues)
    {
        this.IMS = IMS;
        this.searchFactor = searchFactor;
        this.searchValues = searchValues;
    }

    @Override
    public TaskStatus run(TaskProgressMonitor progressMonitor)
        throws Exception
    {
        identities = IMS.readIdentities(searchFactor, searchValues);

        return TaskStatus.OK_STATUS;
    }

    public TeamFoundationIdentity[][] getIdentities()
    {
        return identities;
    }
}
