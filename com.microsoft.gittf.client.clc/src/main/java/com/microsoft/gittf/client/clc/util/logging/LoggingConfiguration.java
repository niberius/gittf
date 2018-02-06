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
import java.text.MessageFormat;

import com.microsoft.gittf.client.clc.ProductInformation;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;
import com.microsoft.tfs.logging.config.ClassloaderConfigurationProvider;
import com.microsoft.tfs.logging.config.Config;
import com.microsoft.tfs.logging.config.EnableReconfigurationPolicy;
import com.microsoft.tfs.logging.config.FromFileConfigurationProvider;
import com.microsoft.tfs.logging.config.MultiConfigurationProvider;
import com.microsoft.tfs.logging.config.ResetConfigurationPolicy;

/**
 * This class implements git-tfs-specific logging configuration.
 * 
 */
public class LoggingConfiguration
{
    private static boolean configured = false;

    public synchronized static void configure()
    {
        if (configured)
        {
            return;
        }

        configured = true;

        /*
         * Find the Configuration directory under the default config location.
         */
        FilesystemPersistenceStore logConfLocation =
            DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore();

        /*
         * The MultiConfigurationProvider holds multiple configuration methods
         * for the logging system. Each method is tried in sequence until one
         * successfully produces a logging configuration file.
         * 
         * This allows us to look for a logging configuration file in the file
         * system, and then fall back to a built-in configuration if no custom
         * file is present.
         */
        MultiConfigurationProvider mcp = new MultiConfigurationProvider();

        /* Set up the names of the log properties files */
        final String propertiesConfigFile =
            MessageFormat.format("log4j-{0}.properties", ProductInformation.getProductName()); //$NON-NLS-1$
        final String xmlConfigFile = MessageFormat.format("log4j-{0}.xml", ProductInformation.getProductName()); //$NON-NLS-1$

        /*
         * Look for log4j-git-tfs.properties and log4j-git-tfs.xml in the
         * "common" directory.
         */
        mcp.addConfigurationProvider(new FromFileConfigurationProvider(new File[]
        {
            logConfLocation.getItemFile(propertiesConfigFile), logConfLocation.getItemFile(xmlConfigFile),
        }));

        /*
         * Load log4j-git-tfs.properties from the classloader that loaded
         * LoggingConfiguration (the classloader for Core)
         */
        mcp.addConfigurationProvider(new ClassloaderConfigurationProvider(
            LoggingConfiguration.class.getClassLoader(),
            new String[]
            {
                propertiesConfigFile
            }));

        /*
         * Safe off the current thread context classloader since we need to
         * re-set it temporarily
         */
        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            /*
             * Setting the thread context classloader to the class loader who
             * loaded TELoggingConfiguration allows Log4J to load custom
             * Appenders from this class loader (eg TEAppender)
             */
            Thread.currentThread().setContextClassLoader(LoggingConfiguration.class.getClassLoader());

            /*
             * Call into the configuration API in com.microsoft.tfs.logging
             */
            Config.configure(
                mcp,
                EnableReconfigurationPolicy.DISABLE_WHEN_EXTERNALLY_CONFIGURED,
                ResetConfigurationPolicy.RESET_EXISTING);
        }
        finally
        {
            /*
             * Always reset the thread context classloader
             */
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }
    }
}
