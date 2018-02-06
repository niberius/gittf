package com.microsoft.gittf.core.impl;

import com.microsoft.gittf.core.interfaces.IdentityManagementService;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.webservices.IIdentityManagementService;
import com.microsoft.tfs.core.clients.webservices.IdentitySearchFactor;
import com.microsoft.tfs.core.clients.webservices.MembershipQuery;
import com.microsoft.tfs.core.clients.webservices.ReadIdentityOptions;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;

public class TfsIdentityManagementService
    implements IdentityManagementService
{
    final IIdentityManagementService IMS;

    public TfsIdentityManagementService(final TFSConnection connection)
    {
        this.IMS = (IIdentityManagementService) connection.getClient(IIdentityManagementService.class);
    }

    public TeamFoundationIdentity[][] readIdentities(
        final IdentitySearchFactor searchFactor,
        final String[] factorValues)
    {
        return IMS.readIdentities(searchFactor, factorValues, MembershipQuery.NONE, ReadIdentityOptions.NONE);
    }

    public TeamFoundationIdentity readIdentity(final IdentitySearchFactor searchFactor, final String factorValue)
    {
        return IMS.readIdentity(searchFactor, factorValue, MembershipQuery.NONE, ReadIdentityOptions.NONE);
    }
}
