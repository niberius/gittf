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

package com.microsoft.gittf.client.tfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.microsoft.gittf.client.tfs.Library.InvalidConfigurationException;
import com.microsoft.gittf.client.tfs.Library.Logger;

/**
 * 
 * Test environment settings; these are pulled from a TestEnvironment.xml
 * configuration file (xml).
 * 
 * @author jpresto
 * 
 */
public class TestEnvironment
{
    // Define the constants we need.
    private final static String DEFAULTTESTENVIRONMENTFILE = "C:\\TestEnvironment.xml"; //$NON-NLS-1$
    private final static String TESTCONFIGURATIONENVIRONMENTVARIABLENAME = "TestConfiguraitonFile"; //$NON-NLS-1$
    private final static String CONFIGURATIONPARAMETERMISSINGMESSAGE =
        "{0} value not defined TestEnvironment.xml configuration file"; //$NON-NLS-1$

    // Value which we pull from our configuration file.
    private static String deploymentUrl = ""; //$NON-NLS-1$
    private static String defaultCollectionName = ""; //$NON-NLS-1$
    private static String collectionUrl = ""; //$NON-NLS-1$
    private static String teamProjectName = ""; //$NON-NLS-1$
    private static Hashtable<String, String> testVariables = new Hashtable<String, String>();;

    /**
     * Perform any initialization we need.
     */
    public static void initialize()
    {
        // read in the xml file and parse it
        readConfiguration();
    }

    /**
     * Get the configuration file location.
     * 
     * @return the file location for the test environment settings.
     * @throws FileNotFoundException
     */
    private static String getConfigurationFile()
        throws FileNotFoundException
    {
        String configurationFileName = System.getenv(TESTCONFIGURATIONENVIRONMENTVARIABLENAME);

        if (configurationFileName == null || configurationFileName.length() == 0)
        {
            Logger.log(MessageFormat.format(
                "Did not find environment variable '{0}'; using default value of '{2}'", TESTCONFIGURATIONENVIRONMENTVARIABLENAME, DEFAULTTESTENVIRONMENTFILE)); //$NON-NLS-1$
            configurationFileName = DEFAULTTESTENVIRONMENTFILE;
        }
        else
        {
            Logger.log(MessageFormat.format(
                "Found environment variable '{0}' = '{1}'", TESTCONFIGURATIONENVIRONMENTVARIABLENAME, configurationFileName)); //$NON-NLS-1$
        }

        File file = new File(configurationFileName);
        if (!file.exists())
        {
            Logger.log(MessageFormat.format("Did not find a configuration file here: '{0}'", configurationFileName)); //$NON-NLS-1$
            Logger.log(MessageFormat.format(
                "Please create a TestEnvironment.xml file and place it in c:\\ or set the environment variable '{0}' equal to the file full path", TESTCONFIGURATIONENVIRONMENTVARIABLENAME)); //$NON-NLS-1$

            throw new FileNotFoundException();
        }

        return configurationFileName;
    }

    /**
     * Get the xml content and set the values locally. The environment variable
     * %TestConfigurationFile% needs to be set to pull the file correctly.
     * 
     * @return true if reading was successful
     */
    protected static Boolean readConfiguration()
    {
        try
        {
            // read in the xml file
            File file = new File(getConfigurationFile());
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            // set the deployment url
            NodeList list = doc.getElementsByTagName(TestEnvironmentConstants.CONFIGURATIONSERVERINPUT);
            NamedNodeMap map = list.item(0).getAttributes();
            Node urlNode = map.getNamedItem("Url"); //$NON-NLS-1$
            deploymentUrl = urlNode.getNodeValue();

            // get the collection name
            NodeList childNodes = list.item(0).getChildNodes();
            Node collectionNode = null;
            for (int i = 0; i < childNodes.getLength(); i++)
            {
                if (childNodes.item(i).getNodeName() == TestEnvironmentConstants.PROJECTCOLLECTIONINPUT)
                {
                    collectionNode = childNodes.item(i);
                    defaultCollectionName =
                        childNodes.item(i).getAttributes().getNamedItem(TestEnvironmentConstants.NAME).getNodeValue();
                    break;
                }
            }

            collectionUrl = deploymentUrl + "/" + defaultCollectionName; //$NON-NLS-1$

            if (collectionNode == null)
            {
                Logger.log("Collection node missing from the test environment configuration file"); //$NON-NLS-1$
                return false;
            }

            // get team project
            NodeList collectionChildren = collectionNode.getChildNodes();
            for (int i = 0; i < collectionChildren.getLength(); i++)
            {
                if (collectionChildren.item(i).getNodeName() == TestEnvironmentConstants.TEAMPROJECTINPUT)
                {
                    teamProjectName =
                        collectionChildren.item(i).getAttributes().getNamedItem(TestEnvironmentConstants.NAME).getNodeValue();
                    break;
                }
            }

            // read the name/value pairs from configuration
            readTestVariables(doc);

            // log the details we read in; this is useful for debugging later if
            // needed
            logTestDetails();

            // verify the name/value pairs needed were provided
            verifyConfigurationSettings();

            return true;
        }
        catch (Exception e)
        {
            Logger.log(MessageFormat.format(
                "Exception trying to parse the TestEnvironment.xml file: {0}", e.getMessage())); //$NON-NLS-1$
            return false;
        }
    }

    /**
     * Get the value of one of the configured (from configuration file) values
     * for the given key.
     * 
     * @param key
     *        key of the key/value pair we are looking for
     * @return value for that key
     */
    public static String getTestVariableValue(String key)
    {
        return testVariables.get(key);
    }

    /**
     * Verify the name/value pairs we require have been provided.
     * 
     * @throws InvalidConfigurationException
     */
    private static void verifyConfigurationSettings()
        throws InvalidConfigurationException
    {
        if (!testVariables.containsKey(TestEnvironmentConstants.VARIABLEGITEXEPATH))
        {
            throw new InvalidConfigurationException(MessageFormat.format(
                CONFIGURATIONPARAMETERMISSINGMESSAGE,
                TestEnvironmentConstants.VARIABLEGITEXEPATH));
        }

        if (!testVariables.containsKey(TestEnvironmentConstants.VARIABLEGITTFEXEPATH))
        {
            throw new InvalidConfigurationException(MessageFormat.format(
                CONFIGURATIONPARAMETERMISSINGMESSAGE,
                TestEnvironmentConstants.VARIABLEGITTFEXEPATH));
        }

        if (!testVariables.containsKey(TestEnvironmentConstants.VARIBLEGITREPOSITORYROOTPATH))
        {
            throw new InvalidConfigurationException(MessageFormat.format(
                CONFIGURATIONPARAMETERMISSINGMESSAGE,
                TestEnvironmentConstants.VARIBLEGITREPOSITORYROOTPATH));
        }
    }

    /**
     * Get the collection url we are targeting.
     * 
     * @return collection url we are targeting
     */
    protected static String getCollectionUrl()
    {
        return collectionUrl;
    }

    /**
     * Read the name/value pairs from the configuration file.
     */
    private static void readTestVariables(Document doc)
    {
        NodeList list = doc.getElementsByTagName(TestEnvironmentConstants.TESTVARIABLES);
        NodeList childNodes = list.item(0).getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            if (childNodes.item(i).getNodeName() == TestEnvironmentConstants.VALUE)
            {
                String key =
                    childNodes.item(i).getAttributes().getNamedItem(TestEnvironmentConstants.KEY).getNodeValue();
                String value = childNodes.item(i).getTextContent();
                testVariables.put(key, value);
            }
        }
    }

    /**
     * Log the details of the configuration file we have read in.
     */
    private static void logTestDetails()
    {
        Logger.logBreak();
        Logger.log("Test Inputs:"); //$NON-NLS-1$
        Logger.logBreak();
        Logger.log(MessageFormat.format("DeploymentUrl:     {0}", deploymentUrl)); //$NON-NLS-1$
        Logger.log(MessageFormat.format("CollectionName:    {0}", defaultCollectionName)); //$NON-NLS-1$
        Logger.log(MessageFormat.format("CollectionUrl:     {0}", collectionUrl)); //$NON-NLS-1$
        Logger.log(MessageFormat.format("Team Project:      {0}", teamProjectName)); //$NON-NLS-1$

        Logger.log("Test Variables:"); //$NON-NLS-1$
        Enumeration<String> variableKeys = testVariables.keys();
        while (variableKeys.hasMoreElements())
        {
            String key = (String) variableKeys.nextElement();
            Logger.log(MessageFormat.format("    {0}:  {1}", key, testVariables.get(key))); //$NON-NLS-1$
        }
        Logger.logBreak();
    }

    /**
     * Return the git exe folder where git.exe is located.
     */
    public static String getGitExeFolder()
    {
        return testVariables.get(TestEnvironmentConstants.VARIABLEGITEXEPATH);
    }

    /**
     * Return the full path for git.exe.
     */
    public static String getGitExeFullPath()
    {
        File folder = new File(getGitExeFolder());
        File fileAndFolder = new File(folder, TestEnvironmentConstants.GITEXE);
        return fileAndFolder.getPath();
    }

    /**
     * Get the git-tf.cmd folder.
     */
    public static String getGitTfExeFolder()
    {
        return testVariables.get(TestEnvironmentConstants.VARIABLEGITTFEXEPATH);
    }

    /**
     * Get the full path for git-tf.cmd.
     */
    public static String getGitTfExeFullPath()
    {
        File folder = new File(getGitTfExeFolder());
        File fileAndFolder = new File(folder, TestEnvironmentConstants.GITTFEXE);
        return fileAndFolder.getPath();
    }

    /**
     * Get the repository path - or the test root for testing.
     */
    public static String getGitRepositoryRootPath()
    {
        return testVariables.get(TestEnvironmentConstants.VARIBLEGITREPOSITORYROOTPATH);
    }

    /**
     * Get the team project name.
     */
    public static String getTfsTeamProjectName()
    {
        return teamProjectName;
    }
}
