package com.microsoft.gittf.core.identity;

import org.eclipse.jgit.lib.PersonIdent;

public class GitUser
    implements Comparable<GitUser>
{
    private final String name;
    private final String email;

    public GitUser(final PersonIdent personalIdent)
    {
        this.name = personalIdent.getName();
        this.email = personalIdent.getEmailAddress();
    }

    public GitUser(final String name, final String email)
    {
        this.name = name;
        this.email = email;
    }

    public String getName()
    {
        return name;
    }

    public String getEmail()
    {
        return email;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode() << 31 + email.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        final GitUser o = (GitUser) obj;
        return name.equalsIgnoreCase(o.name) ? email.equalsIgnoreCase(o.email) : false;
    }

    public int compareTo(final GitUser o)
    {
        final int emailComapreResult = email.compareToIgnoreCase(o.email);

        if (emailComapreResult == 0)
        {
            return name.compareToIgnoreCase(o.name);
        }
        else
        {
            return emailComapreResult;
        }
    }

    @Override
    public String toString()
    {
        return name + " <" + email + ">"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
