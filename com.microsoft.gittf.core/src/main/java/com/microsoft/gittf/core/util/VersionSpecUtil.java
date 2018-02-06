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

package com.microsoft.gittf.core.util;

import java.text.SimpleDateFormat;

import com.microsoft.gittf.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.DateVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LabelVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;

public final class VersionSpecUtil
{
    private VersionSpecUtil()
    {
    }

    public static VersionSpec parseVersionSpec(final String value)
    {
        if (value == null)
        {
            return LatestVersionSpec.INSTANCE;
        }

        if ("latest".equalsIgnoreCase(value)) //$NON-NLS-1$
        {
            return LatestVersionSpec.INSTANCE;
        }

        VersionSpec versionSpec = VersionSpec.parseSingleVersionFromSpec(value, null);

        if (versionSpec == null)
        {
            versionSpec = LatestVersionSpec.INSTANCE;
        }

        return versionSpec;
    }

    public static String getDescription(final VersionSpec versionSpec)
    {
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$

        if (versionSpec instanceof LatestVersionSpec)
        {
            return Messages.getString("VersionSpecUtil.Latest"); //$NON-NLS-1$
        }
        else if (versionSpec instanceof ChangesetVersionSpec)
        {
            return Messages.formatString("VersionSpecUtil.ChangesetFormat", //$NON-NLS-1$
                Integer.toString(((ChangesetVersionSpec) versionSpec).getChangeset()));
        }
        else if (versionSpec instanceof DateVersionSpec)
        {
            return Messages.formatString("VersionSpecUtil.DateFormat", //$NON-NLS-1$
                SimpleDateFormat.getDateTimeInstance().format(((DateVersionSpec) versionSpec).getDate().getTime()));
        }
        else if (versionSpec instanceof LabelVersionSpec)
        {
            return Messages.formatString("VersionSpecUtil.LabelFormat", ((LabelVersionSpec) versionSpec).getLabel()); //$NON-NLS-1$
        }
        else if (versionSpec instanceof WorkspaceVersionSpec)
        {
            return Messages.formatString(
                "VersionSpecUtil.WorkspaceFormat", ((WorkspaceVersionSpec) versionSpec).getName()); //$NON-NLS-1$
        }

        throw new RuntimeException("unknown versionspec type"); //$NON-NLS-1$
    }
}
