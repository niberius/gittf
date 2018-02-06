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

package com.microsoft.gittf.client.tfs.Library;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.gittf.client.tfs.TestEnvironment;
import com.microsoft.gittf.client.tfs.TestEnvironmentConstants;

/**
 * Base class for the command line applications
 * 
 * @author jpresto
 * 
 */
public abstract class CommandBase
{
    // members
    private String standardOutput = null;
    private String stardardError = null;
    private String commandInput = null;
    private int exitValue = -101;
    private final ProcessBuilder processBuilder;

    public CommandBase()
    {
        // create process builder (we need to create this here so that the
        // caller can add environment settings before kicking off the actual
        // process
        processBuilder = new ProcessBuilder();
    }

    /**
     * Get the executable full path for the application to run.
     */
    public abstract String getExeFullPath();

    /**
     * Get the executable path (folder) for for the application.
     */
    public abstract String getExeFolder();

    /**
     * Run the command; typically
     */
    public abstract int runCommand()
        throws IOException,
            InterruptedException;

    /**
     * Get the working folder for running the application.
     */
    public abstract String getWorkingFolder();

    /**
     * Set the working folder for running the application.
     */
    public abstract void getWorkingFolder(String workingFolder);

    /**
     * Get the arguments for the application.
     */
    public abstract String getProcessArgs();

    /**
     * Add custom environment variable settings.
     */
    public void addEnvironmentVariable(final String key, final String value)
    {
        processBuilder.environment().put(key, value);
    }

    /**
     * Add the environment variable path.
     */
    public void addEnvironmentPath(String newFolderPath)
    {
        String currentPath = (String) processBuilder.environment().get(TestEnvironmentConstants.PATH);

        processBuilder.environment().put(
            TestEnvironmentConstants.PATH,
            MessageFormat.format("{0}{1}{2}", newFolderPath, System.getProperty("path.separator"), currentPath)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Run the program.
     */
    public int run()
        throws IOException,
            InterruptedException
    {
        processBuilder.environment().put(
            TestEnvironmentConstants.VARIABLEJAVAHOME,
            TestEnvironment.getTestVariableValue(TestEnvironmentConstants.VARIABLEJAVAHOME));

        commandInput = MessageFormat.format("{0} {1}", getExeFullPath(), getProcessArgs()); //$NON-NLS-1$

        // process builder expects each arg to be a different value.. i.e. you
        // cannot just concat the args together with spaces between them
        List<String> list = new ArrayList<String>();
        list.add(getExeFullPath());
        String[] splitArgs = getProcessArgs().split(" "); //$NON-NLS-1$
        for (int i = 0; i < splitArgs.length; i++)
        {
            list.add(splitArgs[i]);
        }

        processBuilder.command(list);
        processBuilder.directory(new File(getWorkingFolder()));

        try
        {
            Logger.log(MessageFormat.format("Starting: {0} {1}", getExeFullPath(), getProcessArgs())); //$NON-NLS-1$

            // kick off the command
            Process process = processBuilder.start();
            process.waitFor();

            // get the output
            String line;
            standardOutput = ""; //$NON-NLS-1$
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = bufferedReader.readLine()) != null)
            {
                standardOutput = standardOutput + line + System.getProperty("line.separator"); //$NON-NLS-1$
            }

            // get the error
            stardardError = ""; //$NON-NLS-1$
            bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = bufferedReader.readLine()) != null)
            {
                stardardError = stardardError + line + System.getProperty("line.separator"); //$NON-NLS-1$
            }

            exitValue = process.exitValue();
        }
        catch (IOException e1)
        {
            Logger.logException(e1);
            throw e1;
        }
        catch (InterruptedException e2)
        {
            Logger.logException(e2);
            throw e2;
        }

        return exitValue;
    }

    /**
     * Return the standard out of the application.
     */
    public String getStandardOut()
    {
        if (standardOutput == null)
        {
            return ""; //$NON-NLS-1$
        }
        return standardOutput;
    }

    /**
     * Return the standard error of the application.
     */
    public String getStandardErr()
    {
        if (stardardError == null)
        {
            return ""; //$NON-NLS-1$
        }
        return stardardError;
    }

    /**
     * Get the command input (this is the program plus arguments).
     */
    public String getCommandInput()
    {
        if (commandInput == null)
        {
            return ""; //$NON-NLS-1$
        }
        return commandInput;
    }

    /**
     * Return the exit code of the application we just ran.
     */
    public int getExitCode()
    {
        return exitValue;
    }

    /**
     * Log the results of the application we just ran.
     */
    public void logResults()
    {
        Logger.log(MessageFormat.format("Working Folder: {0}", getWorkingFolder())); //$NON-NLS-1$
        Logger.log(MessageFormat.format("Command Input:  {0}", getCommandInput())); //$NON-NLS-1$
        Logger.log(MessageFormat.format("Exit:           {0}", getExitCode())); //$NON-NLS-1$
        Logger.log("Standard Output", getStandardOut()); //$NON-NLS-1$ 
        Logger.log("Standard Error", getStandardErr()); //$NON-NLS-1$
    }

    /**
     * Log details about the process and environment.
     */
    public void logDetails()
    {
        Logger.logBreak();
        Logger.logBreak();
        Logger.log("Process Builder and Environment Information"); //$NON-NLS-1$
        Logger.logBreak();
        // display environment variables we are interested in
        String httpProxy = (String) processBuilder.environment().get(TestEnvironmentConstants.VARIABLEHTTPPROXY);
        String javaHome = (String) processBuilder.environment().get(TestEnvironmentConstants.VARIABLEJAVAHOME);
        String path = (String) processBuilder.environment().get(TestEnvironmentConstants.PATH);
        Logger.log(MessageFormat.format("    http_proxy:  {0}", httpProxy)); //$NON-NLS-1$
        Logger.log(MessageFormat.format("    JAVA_HOME:   {0}", javaHome)); //$NON-NLS-1$
        Logger.log("    PATH (split):"); //$NON-NLS-1$
        String[] paths = path.split(";"); //$NON-NLS-1$
        for (int i = 0; i < paths.length; i++)
        {
            Logger.log(MessageFormat.format("        {0}", paths[i])); //$NON-NLS-1$
        }
        Logger.logBreak();
        Logger.logBreak();
    }
}
