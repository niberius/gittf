package com.microsoft.gittf.core.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.microsoft.gittf.core.identity.GitUser;
import com.microsoft.gittf.core.identity.TfsUser;
import com.microsoft.gittf.core.identity.UserMap;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.tfs.core.clients.webservices.IdentitySearchFactor;

public class MockUserMap
    extends UserMap
{
    private final String[] userMapFile;
    private final GitUser[] gitUsers;
    private final TfsUser[] tfsUsers;
    private String[] newUserMapFile;

    public MockUserMap(
        final String userMapPath,
        final String[] userMapFile,
        final GitUser[] gitUsers,
        final TfsUser[] tfsUsers)
    {
        super(userMapPath);

        this.userMapFile = userMapFile;
        this.gitUsers = gitUsers;
        this.tfsUsers = tfsUsers;
    }

    @Override
    protected List<String> readUserMapFile()
    {
        return Arrays.asList(userMapFile);
    }

    @Override
    protected void writeUserMapFile(List<String> fileLines)
    {
        newUserMapFile = fileLines.toArray(new String[fileLines.size()]);
    }

    @Override
    public void addGitUsers()
    {
        for (final GitUser gitUser : gitUsers)
        {
            addGitUser(gitUser);
        }
    }

    public String[] getNewUserMapFile()
    {
        return newUserMapFile;
    }

    @Override
    protected Map<String, List<TfsUser>> findTfsUsers(
        TaskProgressMonitor progressMonitor,
        List<String> searchValues,
        IdentitySearchFactor searchFactor)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
