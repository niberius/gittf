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

import org.eclipse.jgit.lib.Repository;

import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

/**
 * Updates the local version information for a workspace to a specific version
 * 
 * This task basically fakes the server into believing that we have the
 * specified version of the file, without having to download all the files from
 * the server
 */
public class UpdateLocalVersionToSpecificVersionsTask
    extends UpdateLocalVersionTask
{
    private final VersionSpec versionSpec;
    private final Repository repository;

    private GetOperation[][] tfsGetOperations;

    /**
     * Constructor
     * 
     * @param workspace
     *        the workspace to set the local version information for
     * @param repository
     *        the repository that this workspace is mapped to
     * @param versionSpec
     *        the version spec to use
     */
    public UpdateLocalVersionToSpecificVersionsTask(
        final Workspace workspace,
        final Repository repository,
        final VersionSpec versionSpec)
    {
        super(workspace);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$

        this.repository = repository;
        this.versionSpec = versionSpec;
    }

    @Override
    protected GetOperation[][] getGetOperations()
    {
        if (tfsGetOperations == null)
        {
            tfsGetOperations = getVersionSpecGetOps();
        }

        return tfsGetOperations;
    }

    /**
     * Gets the getOps for the version spec specified
     * 
     * @return
     */
    private GetOperation[][] getVersionSpecGetOps()
    {
        GitTFConfiguration configuration = GitTFConfiguration.loadFrom(repository);

        /*
         * Use the web service layer to find out the getOps needed to update
         * this workspace with local version information
         */
        GetOperation[][] tfsGetOperations =
            workspace.getClient().getWebServiceLayer().get(
                workspace.getName(),
                workspace.getOwnerName(),
                new GetRequest[]
                {
                    new GetRequest(new ItemSpec(configuration.getServerPath(), RecursionType.FULL), versionSpec)
                },
                0,
                GetOptions.NO_DISK_UPDATE.combine(GetOptions.GET_ALL),
                null,
                null,
                false);

        return tfsGetOperations;
    }
}
