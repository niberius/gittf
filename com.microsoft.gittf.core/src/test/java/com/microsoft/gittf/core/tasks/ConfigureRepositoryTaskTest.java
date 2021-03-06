/*
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
 */

package com.microsoft.gittf.core.tasks;

import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.tasks.framework.NullTaskProgressMonitor;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.test.Util;
import junit.framework.TestCase;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import java.net.URI;

public class ConfigureRepositoryTaskTest
        extends TestCase {
    private Repository repository;

    protected void setUp()
            throws Exception {
        Util.setUp(getName());
        repository = Util.initializeGitRepo(getName());
    }

    protected void tearDown()
            throws Exception {
        Util.tearDown(getName());
    }

    @Test
    public void testSimpleConfigure()
            throws Exception {
        assertNotNull(repository);

        URI projectCollectionURI = new URI("http://fakeCollection:8080/tfs/DefaultCollection");
        String tfsPath = "$/";

        ConfigureRepositoryTask configTask = new ConfigureRepositoryTask(repository, projectCollectionURI, tfsPath);
        configTask.setDeep(true);
        configTask.setIncludeMetaData(true);
        TaskStatus configTaskStatus = configTask.run(new NullTaskProgressMonitor());

        assertTrue(configTaskStatus.isOK());

        GitTFConfiguration gitRepoServerConfig = GitTFConfiguration.loadFrom(repository);

        assertEquals(gitRepoServerConfig.getServerURI(), projectCollectionURI);
        assertEquals(gitRepoServerConfig.getServerPath(), tfsPath);
        assertEquals(gitRepoServerConfig.getDeep(), true);
        assertEquals(gitRepoServerConfig.getIncludeMetaData(), true);
    }

    @Test
    public void testComplexConfigure()
            throws Exception {
        assertNotNull(repository);

        String[] uriCombinations = new String[]
                {
                        "http://fakeCollection:8080/tfs/DefaultCollection",
                        "http://fakeCollection:8080/tfs",
                        "http://fakeCollection/tfs",
                        "http://fakeCollection:8080/tfs/DefaultCollection/vdir",
                        "http://fakeCollection:8080/tfs/vdir",
                        "http://fakeCollection/tfs/vdir",
                };

        String[] serverValidPathCombinations = new String[]
                {
                        "$/", "$/Folder", "$/Folder/Folder2/Folder"
                };

        String[] serverInvalidPathCombinations = new String[]
                {
                        " ", "Folder", "Folder/Folder2/Folder", "*"
                };

        for (String uri : uriCombinations) {
            URI projectCollectionURI = new URI(uri);

            verifyServerPaths(projectCollectionURI, serverValidPathCombinations, true);

            verifyServerPaths(projectCollectionURI, serverInvalidPathCombinations, false);
        }
    }

    private void verifyServerPaths(URI projectCollectionURI, String[] serverPaths, boolean shouldPass) {
        for (String tfsPath : serverPaths) {
            ConfigureRepositoryTask configTask = new ConfigureRepositoryTask(repository, projectCollectionURI, tfsPath);
            TaskStatus configTaskStatus = configTask.run(new NullTaskProgressMonitor());

            assertTrue(configTaskStatus.isOK() == shouldPass);

            if (shouldPass) {
                GitTFConfiguration gitRepoServerConfig = GitTFConfiguration.loadFrom(repository);

                assertEquals(gitRepoServerConfig.getServerURI(), projectCollectionURI);
                assertEquals(gitRepoServerConfig.getServerPath(), tfsPath);
                assertEquals(gitRepoServerConfig.getDeep(), GitTFConstants.GIT_TF_DEFAULT_DEEP);
                assertEquals(gitRepoServerConfig.getIncludeMetaData(), GitTFConstants.GIT_TF_DEFAULT_INCLUDE_METADATA);
            }
        }
    }
}
