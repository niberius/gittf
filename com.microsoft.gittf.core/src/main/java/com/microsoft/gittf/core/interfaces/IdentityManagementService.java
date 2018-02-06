package com.microsoft.gittf.core.interfaces;

import com.microsoft.tfs.core.clients.webservices.IdentitySearchFactor;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;

public interface IdentityManagementService
{
    TeamFoundationIdentity[][] readIdentities(final IdentitySearchFactor searchFactor, final String[] factorValues);

    TeamFoundationIdentity readIdentity(final IdentitySearchFactor searchFactor, final String factorValue);
}
