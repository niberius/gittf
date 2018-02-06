package com.microsoft.gittf.core.identity;

import com.microsoft.tfs.util.StringHelpers;

public class TfsUser
    implements Comparable<TfsUser>
{
    private final String name;
    private final String displayName;

    public TfsUser(final String name)
    {
        this(name, null);
    }

    public TfsUser(final String name, final String displayName)
    {
        this.name = name;
        this.displayName = StringHelpers.isNullOrEmpty(displayName) ? name : displayName;
    }

    public String getName()
    {
        return name;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode() << 31 + displayName.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        final TfsUser o = (TfsUser) obj;
        return name.equalsIgnoreCase(o.name) ? displayName.equalsIgnoreCase(o.displayName) : false;
    }

    public int compareTo(final TfsUser o)
    {
        final int displayNameComapreResult = displayName.compareToIgnoreCase(o.displayName);

        if (displayNameComapreResult == 0)
        {
            return name.compareToIgnoreCase(o.name);
        }
        else
        {
            return displayNameComapreResult;
        }
    }

    @Override
    public String toString()
    {
        return displayName + " (" + name + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
