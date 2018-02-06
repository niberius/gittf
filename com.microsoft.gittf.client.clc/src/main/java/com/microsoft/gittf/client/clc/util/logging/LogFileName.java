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

package com.microsoft.gittf.client.clc.util.logging;

import java.io.File;
import java.io.FileFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogFileName
    implements Comparable<LogFileName>
{
    private static final String LOGFILE_DATE_FORMAT = "yyyy.MM.dd-HH.mm.ss"; //$NON-NLS-1$

    public static final String LOGFILE_EXTENSION = ".log"; //$NON-NLS-1$

    private final String logType;
    private final Date date;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(LOGFILE_DATE_FORMAT);

    public static FileFilter getFilterForAllLogFiles()
    {
        return new FileFilter()
        {
            public boolean accept(final File pathname)
            {
                return pathname.isFile() && pathname.getName().endsWith(LOGFILE_EXTENSION);
            }
        };
    }

    public static FileFilter getFilterForLogFilesOfTypeForCurrentApplication(final String logType)
    {
        final String prefix = logType;

        return new FileFilter()
        {
            public boolean accept(final File pathname)
            {
                return pathname.isFile()
                    && pathname.getName().startsWith(prefix)
                    && pathname.getName().endsWith(LOGFILE_EXTENSION);
            }
        };
    }

    public static LogFileName parse(final String name)
    {
        if (name == null)
        {
            throw new IllegalArgumentException();
        }

        String[] sections = name.split("-"); //$NON-NLS-1$

        if (sections.length < 4)
        {
            return null;
        }

        String sDate = sections[sections.length - 2] + "-" + sections[sections.length - 1]; //$NON-NLS-1$
        SimpleDateFormat dateFormat = new SimpleDateFormat(LOGFILE_DATE_FORMAT);
        Date date;
        try
        {
            date = dateFormat.parse(sDate);
        }
        catch (ParseException e)
        {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < (sections.length - 3); i++)
        {
            sb.append(sections[i]);
            if (i < (sections.length - 4))
            {
                sb.append("-"); //$NON-NLS-1$
            }
        }

        String logType = sb.toString();

        return new LogFileName(logType, date);
    }

    public LogFileName(final String logType)
    {
        this(logType, new Date());
    }

    private LogFileName(final String logType, final Date date)
    {
        this.logType = logType;
        this.date = date;
    }

    public int compareTo(final LogFileName other)
    {
        int c = logType.compareTo(other.logType);
        if (c == 0)
        {
            c = other.date.compareTo(date);
        }

        return c;
    }

    public String getFileName()
    {
        return logType + "-" + dateFormat.format(date) + LOGFILE_EXTENSION; //$NON-NLS-1$ 
    }

    public File createFileDescriptor(final File location)
    {
        return new File(location, getFileName());
    }

    public Date getDate()
    {
        return date;
    }

    public String getLogType()
    {
        return logType;
    }
}
