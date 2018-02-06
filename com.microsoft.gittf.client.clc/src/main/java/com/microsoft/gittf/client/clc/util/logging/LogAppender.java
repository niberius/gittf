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
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.FileAppender;

import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;
import com.microsoft.tfs.core.persistence.PersistenceStore;
import com.microsoft.tfs.core.persistence.VersionedVendorFilesystemPersistenceStore;
import com.microsoft.tfs.util.FileLastModifiedComparator;
import com.microsoft.tfs.util.locking.AdvisoryFileLock;

/**
 * An extension of Log4J's normal FileAppender with the following properties: 1)
 * The log file is written to the Team Explorer settings directory, instead of
 * the process working directory as is the default with FileAppender. 2) Each
 * LogAppender will use a separate log file (old log files are pruned after a
 * certain threshold is met)
 * 
 */
public class LogAppender
    extends FileAppender
{
    private static final int CLEANUP_THRESHOLD = 5;

    private static final Map<String, File> logTypesToInUseLogFiles = new HashMap<String, File>();

    @Override
    public void setFile(final String logType)
    {
        /*
         * Create the logs directory if it doesn't already exist
         */
        VersionedVendorFilesystemPersistenceStore logStore = getLogLocation();
        try
        {
            logStore.initialize();
        }
        catch (IOException e)
        {
            /*
             * Would be nice to log this, but we don't have logs yet. :) Print
             * the error and continue on; logging may actually succeed.
             */
            e.printStackTrace();
        }

        /*
         * Prune old log files in the directory to keep the litter of the file
         * system down
         */
        cleanup(logType, logStore);

        /*
         * create the log file object
         */
        File logFile = createLogFileObject(logType, logStore.getStoreFile(), true);

        /*
         * call the super implementation in FileAppender with the file's full
         * path
         */
        super.setFile(logFile.getAbsolutePath());
    }

    /**
     * Creates a File object appropriate for use for a log file of the given
     * type.
     * 
     * @param logType
     *        the log type
     * @param location
     *        the location of the log file
     * @param setInUse
     *        true to call setInUseLogFileForLogType with the log file
     * @return a file object representing the log file
     */
    private static File createLogFileObject(final String logType, final File location, final boolean setInUse)
    {
        LogFileName logFileName = new LogFileName(logType);

        File logFile = logFileName.createFileDescriptor(location);

        if (setInUse)
        {
            setInUseLogFileForLogType(logType, logFile);
        }

        return logFile;
    }

    /**
     * Marks the specified file as being the currently in-use log file for the
     * given log type.
     * 
     * @param logType
     *        the log type
     * @param logFile
     *        the in-use log file
     */
    static void setInUseLogFileForLogType(final String logType, final File logFile)
    {
        synchronized (logTypesToInUseLogFiles)
        {
            logTypesToInUseLogFiles.put(logType, logFile);
        }
    }

    /**
     * Obtains the {@link PersistenceStore} where all Team Explorer log files
     * should be stored. This {@link PersistenceStore} object is cached
     * internally.
     * 
     * @return the {@link PersistenceStore} described above
     */
    private static VersionedVendorFilesystemPersistenceStore getLogLocation()
    {
        return (VersionedVendorFilesystemPersistenceStore) DefaultPersistenceStoreProvider.INSTANCE.getLogPersistenceStore();
    }

    private void cleanup(final String logType, final FilesystemPersistenceStore logStore)
    {
        /*
         * The basic algorithm here is to get an exclusive lock on the settings
         * location. If that exclusive lock can't be had, we return without
         * doing any cleanup (some other instance of the application is
         * currently performing cleanup on this directory).
         */

        AdvisoryFileLock lock = null;

        try
        {
            lock = logStore.getStoreLock(false);

            /*
             * A null lock means the lock was not immediately available.
             */
            if (lock == null)
            {
                return;
            }

            /*
             * Here's the call to actually perform the cleanup on the directory.
             * At this point we know we have the exclusive lock.
             */
            doCleanup(logType, logStore);
        }
        catch (InterruptedException e)
        {
            /*
             * Shouldn't ever happen because we aren't blocking on getting the
             * lock.
             */
            return;
        }
        catch (IOException e)
        {
            /*
             * Exception trying to get lock - return without doing cleanup
             */
            return;
        }
        finally
        {
            try
            {
                /*
                 * Always release the lock
                 */
                if (lock != null)
                {
                    lock.release();
                }
            }
            catch (IOException e)
            {
            }
        }
    }

    private void doCleanup(final String logType, final FilesystemPersistenceStore logFileLocation)
    {
        File[] logFiles = getAllLogFilesForLogType(logType, logFileLocation.getStoreFile(), true);

        /*
         * If the number of files is not under the cleanup threshold, this
         * method has nothing to do
         */
        if (logFiles.length < CLEANUP_THRESHOLD)
        {
            return;
        }

        /*
         * Attempt to delete enough files to bring us below the cleanup
         * threshold. If the deletes don't succeed, that's fine, as another
         * instance may currently have the file open and locked.
         */
        int numToDelete = logFiles.length - CLEANUP_THRESHOLD + 1;
        int numDeleted = 0;
        for (int i = 0; i < logFiles.length && numDeleted < numToDelete; i++)
        {
            if (logFiles[i].delete())
            {
                ++numDeleted;
            }
        }
    }

    /**
     * Returns all currently existing log files of a given log type residing in
     * a given directory. The result array is sorted by the last modified date
     * of the files. If the sortAscending option is true, earlier last modified
     * dates come first.
     * 
     * @param logType
     *        the log type
     * @param location
     *        the directory
     * @param sortAscending
     *        true to sort last modified dates in ascending order
     * @return matched Files
     */
    @SuppressWarnings("unchecked")
    private static File[] getAllLogFilesForLogType(
        final String logType,
        final File location,
        final boolean sortAscending)
    {
        File[] files = location.listFiles(LogFileName.getFilterForLogFilesOfTypeForCurrentApplication(logType));

        FileLastModifiedComparator c = new FileLastModifiedComparator();
        c.setAscending(sortAscending);
        Arrays.sort(files, c);

        return files;
    }
}
