package com.microsoft.gittf.core.identity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.gittf.core.impl.TfsIdentityManagementService;
import com.microsoft.gittf.core.interfaces.IdentityManagementService;
import com.microsoft.gittf.core.tasks.SearchIdentitesTask;
import com.microsoft.gittf.core.tasks.framework.TaskExecutor;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.CommitWalker.CommitDelta;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.webservices.IdentitySearchFactor;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.util.tasks.CanceledException;

public class TfsUserMap
    extends UserMap
{
    final Log log = LogFactory.getLog(TfsUserMap.class.getName());

    private static final String UNAUTHENTICATED_IDENTITY = "Microsoft.TeamFoundation.UnauthenticatedIdentity"; //$NON-NLS-1$
    private static final String CLAIMS_IDENTITY = "Microsoft.IdentityModel.Claims.ClaimsIdentity"; //$NON-NLS-1$
    private static final String WINDOWS_IDENTITY = "System.Security.Principal.WindowsIdentity"; //$NON-NLS-1$

    private static final String SCHEMA_PROPERTY = "SchemaClassName"; //$NON-NLS-1$
    private static final String USER_SCHEMA = "User"; //$NON-NLS-1$

    private final IdentityManagementService IMS;
    private final List<CommitDelta> commitsToCheckin;

    public TfsUserMap(final TFSConnection connection, final String userMapPath, final List<CommitDelta> commitsToCheckin)
    {
        super(userMapPath);

        this.IMS = new TfsIdentityManagementService(connection);
        this.commitsToCheckin = commitsToCheckin;
    }

    @Override
    public void addGitUsers()
    {
        for (final CommitDelta delta : commitsToCheckin)
        {
            addGitUser(new GitUser(delta.getToCommit().getAuthorIdent()));
        }
    }

    @Override
    protected List<String> readUserMapFile()
        throws Exception
    {
        final List<String> fileLines = new ArrayList<String>();
        final BufferedReader f = new BufferedReader(new FileReader(getUserMapFile()));

        String line = null;
        try
        {
            while ((line = f.readLine()) != null)
            {
                fileLines.add(line);
            }
        }
        finally
        {
            try
            {
                f.close();
            }
            catch (final Exception e)
            {

            }
        }

        return fileLines;
    }

    @Override
    protected void writeUserMapFile(List<String> fileLines)
        throws Exception
    {
        final BufferedWriter f = new BufferedWriter(new FileWriter(getUserMapFile()));

        try
        {
            for (final String line : fileLines)
            {
                f.write(line);
                f.newLine();
            }
        }
        finally
        {
            try
            {
                f.close();
            }
            catch (final Exception e)
            {

            }
        }
    }

    @Override
    protected Map<String, List<TfsUser>> findTfsUsers(
        final TaskProgressMonitor progressMonitor,
        final List<String> searchValues,
        final IdentitySearchFactor searchFactor)
    {
        log.info("TFS identites look-up"); //$NON-NLS-1$
        Check.notNull(IMS, "IMS"); //$NON-NLS-1$

        Map<String, List<TfsUser>> userMap = new HashMap<String, List<TfsUser>>();

        TaskStatus searchStatus;
        SearchIdentitesTask searchTask;

        searchTask = new SearchIdentitesTask(IMS, searchFactor, searchValues.toArray(new String[searchValues.size()]));
        searchStatus = new TaskExecutor(progressMonitor.newSubTask(1)).execute(searchTask);

        if (searchStatus.isOK())
        {
            final TeamFoundationIdentity[][] identitiesList = searchTask.getIdentities();
            if (identitiesList != null)
            {
                for (int k = 0; k < identitiesList.length; k++)
                {
                    log.debug("Search for:    " + searchValues.get(k)); //$NON-NLS-1$

                    final TeamFoundationIdentity[] mappedIdentites = identitiesList[k];
                    if (mappedIdentites != null && mappedIdentites.length > 0)
                    {
                        log.debug("Found:         " + String.valueOf(mappedIdentites.length)); //$NON-NLS-1$

                        final List<TfsUser> tfsUsers = new ArrayList<TfsUser>();

                        for (int j = 0; j < mappedIdentites.length; j++)
                        {
                            final TeamFoundationIdentity identity = mappedIdentites[j];

                            final Iterable<Entry<String, Object>> properties = identity.getProperties();

                            log.debug("Identity:      " + identity.getUniqueName()); //$NON-NLS-1$
                            log.debug("Identity Type: " + identity.getDescriptor().getIdentityType()); //$NON-NLS-1$
                            for (final Entry<String, Object> property : properties)
                            {
                                log.debug(property.getKey() + " = " + property.getValue().toString()); //$NON-NLS-1$
                            }

                            final String type = identity.getDescriptor().getIdentityType();
                            final String schema = (String) identity.getProperty(SCHEMA_PROPERTY);

                            if (identity != null
                                && USER_SCHEMA.equalsIgnoreCase(schema)
                                && (WINDOWS_IDENTITY.equalsIgnoreCase(type) || CLAIMS_IDENTITY.equalsIgnoreCase(type)))
                            {
                                tfsUsers.add(new TfsUser(identity.getUniqueName(), identity.getDisplayName()));
                            }
                            else
                            {
                                log.warn(MessageFormat.format(
                                    "Incorrect identity type \"{0}\" or schema class \"{1}\". Identity ignored.", type, schema)); //$NON-NLS-1$
                            }
                        }

                        if (tfsUsers.size() > 0)
                        {
                            userMap.put(searchValues.get(k), tfsUsers);
                        }
                    }
                    else
                    {
                        log.debug("Found:         0"); //$NON-NLS-1$
                    }
                }
            }
        }
        else if (searchStatus.getSeverity().equals(TaskStatus.CANCEL))
        {
            throw new CanceledException();
        }

        return userMap;
    }
}
