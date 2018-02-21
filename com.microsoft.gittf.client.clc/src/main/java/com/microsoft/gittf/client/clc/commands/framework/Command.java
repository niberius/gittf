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

package com.microsoft.gittf.client.clc.commands.framework;

import com.microsoft.gittf.client.clc.Console;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.ProductInformation;
import com.microsoft.gittf.client.clc.arguments.Argument;
import com.microsoft.gittf.client.clc.arguments.ArgumentOptions;
import com.microsoft.gittf.client.clc.arguments.ValueArgument;
import com.microsoft.gittf.client.clc.arguments.parser.ArgumentCollection;
import com.microsoft.gittf.client.clc.connection.GitTFConnectionAdvisor;
import com.microsoft.gittf.client.clc.util.HelpFormatter;
import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.impl.TfsVersionControlService;
import com.microsoft.gittf.core.interfaces.VersionControlService;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.upgrade.UpgradeManager;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.RepositoryUtil;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorListener;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LabelVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.exceptions.ACSUnauthorizedException;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.exceptions.TFSFederatedAuthException;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.auth.AuthenticationException;
import com.microsoft.tfs.core.ws.runtime.exceptions.EndpointNotFoundException;
import com.microsoft.tfs.core.ws.runtime.exceptions.UnauthorizedException;
import com.microsoft.tfs.jni.ConsoleUtils;
import com.microsoft.tfs.jni.NTLMEngine;
import com.microsoft.tfs.jni.NegotiateEngine;
import com.microsoft.tfs.util.StringHelpers;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.gittf.core.util.Check.notNullOrEmpty;

public abstract class Command {
    protected final Log log = LogFactory.getLog(this.getClass());
    private static final String GIT_DIR = ".git";
    private static final String WORKING_DIRS_SEPARATOR_REGEX = ";";

    protected Console console;

    private Credentials userCredentials;
    private TFSTeamProjectCollection connection;
    private VersionControlClient versionControlClient;

    private VersionControlService versionControlService;

    private Repository gitRepository;
    private Set<Repository> gitRepositories;

    private GitTFConfiguration serverConfiguration;

    private ArgumentCollection arguments = new ArgumentCollection();

    private static Credentials promptForCredentials(Credentials credentials) {
        String username;
        String password;

        if (credentials != null && credentials instanceof UsernamePasswordCredentials) {
            username = ((UsernamePasswordCredentials) credentials).getUsername();
        } else {
            username = promptForUsername();

            if (username == null || username.length() == 0) {
                return null;
            }
        }

        password = promptForPassword();

        return new UsernamePasswordCredentials(username, password);
    }

    private static String promptForUsername() {
        return prompt(Messages.getString("Command.UsernamePrompt"), true);
    }

    private static String promptForPassword() {
        return prompt(Messages.getString("Command.PasswordPrompt"), false);
    }

    private static String prompt(String prompt, boolean echo) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            if (!echo && !ConsoleUtils.getInstance().disableEcho()) {
                System.err.println(Messages.getString("Command.PasswordEchoCouldNotBeDisabled"));
            }

            System.out.print(prompt);

            return reader.readLine();
        } catch (IOException e) {
            /* Do not log, may contain sensitive information */
            return null;
        } finally {
            if (!echo) {
                System.out.println();
                ConsoleUtils.getInstance().enableEcho();
            }
        }
    }

    protected abstract String getCommandName();

    protected final Console getConsole() {
        return console;
    }

    public final void setConsole(final Console console) {
        Check.notNull(console, "console");
        this.console = console;
    }

    protected String getUsage() {
        return Messages.formatString("Command.UsageFormat",
                ProductInformation.getProductName(),
                getCommandName(),
                HelpFormatter.getArgumentSyntax(getPossibleArguments()));
    }

    public void showHelp() {
        System.out.println(HelpFormatter.wrap(getUsage()));
        System.out.println();

        boolean showArguments = false;
        for (Argument arg : getPossibleArguments()) {
            if (!arg.getOptions().contains(ArgumentOptions.HIDDEN)) {
                showArguments = true;
                break;
            }
        }

        if (showArguments) {
            System.out.println(Messages.getString("Command.HelpArguments"));
            System.out.print(HelpFormatter.getArgumentHelp(getPossibleArguments()));

            System.out.println();
        }

        System.out.println(HelpFormatter.wrap(getHelpDescription()));
    }

    public abstract Argument[] getPossibleArguments();

    public abstract String getHelpDescription();

    public ArgumentCollection getArguments() {
        return arguments;
    }

    public void setArguments(ArgumentCollection arguments) {
        Check.notNull(arguments, "arguments");

        this.arguments = arguments;
    }

    public int getDepthFromArguments()
            throws Exception {
        int depth = GitTFConstants.GIT_TF_SHALLOW_DEPTH;

        // Validate arguments
        if (getArguments().contains("depth"))
        {
            String depthValue = ((ValueArgument) getArguments().getArgument("depth")).getValue();

            try {
                depth = Integer.parseInt(depthValue);

                if (depth <= 0) {
                    throw new Exception(Messages.formatString("Command.Argument.Depth.ParseErrorFormat", depthValue));
                }
            } catch (NumberFormatException e) {
                throw new Exception(Messages.formatString("Command.Argument.Depth.ParseErrorFormat", depthValue));
            }
        } else if (getArguments().contains("deep"))
        {
            depth = Integer.MAX_VALUE;
        } else if (getArguments().contains("shallow"))
        {
            depth = 1;
        }

        return depth;
    }

    public boolean getDeepFromArguments() {
        if (getArguments().contains("deep"))
        {
            return true;
        } else if (getArguments().contains("shallow"))
        {
            return false;
        }

        return false;
    }

    public boolean isDepthSpecified() {
        return getArguments().contains("depth")
                || getArguments().contains("deep")
                || getArguments().contains("shallow");
    }

    public boolean getTagFromArguments() {
        return !getArguments().contains("no-tag");
    }

    public boolean getIncludeMetaDataFromArguments() {
        if (getArguments().contains("metadata"))
        {
            return true;
        } else if (getArguments().contains("no-metadata"))
        {
            return false;
        }

        return false;
    }

    public boolean isIncludeMetaDataSpecified() {
        return getArguments().contains("metadata")
                || getArguments().contains("no-metadata");
    }

    protected Repository getRepository()
            throws Exception {
        if (gitRepository == null) {
            if (isMultiRepositories()) {
                // Repository in this class mostly needed for connection creation.
                // Verify, that all repos configured for the same server and set any git repo for connection
                verifyReposConsistency();
                gitRepository = gitRepositories.iterator().next();
            } else {
                gitRepository = getGitRepository(null);
            }
        }
        return gitRepository;
    }

    private Repository getGitRepository(final String gitDir) throws Exception {
        final String determinedGitDir = arguments.contains("git-dir") ?
                ((ValueArgument) arguments.getArgument("git-dir")).getValue() : gitDir;

        final Repository repository = RepositoryUtil.findRepository(determinedGitDir);

        if (repository == null) {
            throw new Exception(Messages.getString("Command.RepositoryNotFound"));
        }

        UpgradeManager.upgradeIfNeccessary(repository);

        return repository;
    }

    protected Set<Repository> getRepositories(final String workingDirsSplitBySemicolon) throws Exception {
        if (gitRepositories == null || gitRepositories.isEmpty()) {
            gitRepositories = new HashSet<>();
            if (workingDirsSplitBySemicolon != null) {
                final String[] workingDirs = workingDirsSplitBySemicolon.split(WORKING_DIRS_SEPARATOR_REGEX);
                for (final String workingDir : workingDirs) {
                    final String gitDir = String.format("%1$s%2$s%3$s", workingDir, File.separator, GIT_DIR);
                    gitRepositories.add(getGitRepository(gitDir));
                }
            } else {
                throw new RuntimeException(Messages.getString("Command.WorkingDirsNotSpecified"));
            }
        }
        return gitRepositories;
    }

    protected GitTFConfiguration getServerConfiguration()
            throws Exception {
        if (serverConfiguration == null) {
            serverConfiguration = GitTFConfiguration.loadFrom(getRepository());

            if (serverConfiguration == null) {
                throw new Exception(Messages.getString("Command.ServerConfigurationNotFound"));
            }
        }

        return serverConfiguration;
    }

    protected Credentials getDefaultCredentials() {
        /*
         * Query the TEE SPNEGO providers to ensure that the appropriate JNI
         * could be loaded.
         */
        if (!NegotiateEngine.getInstance().isAvailable() && !NTLMEngine.getInstance().isAvailable()) {
            log.warn("Could not load native authentication libraries");
        }

        /*
         * Queries the TEE SPNEGO providers to determine if a default ticket
         * exists
         */
        else if (!NegotiateEngine.getInstance().supportsCredentialsDefault()
                && !NTLMEngine.getInstance().supportsCredentialsDefault()) {
            log.info("Default credentials are not available for authentication (no ticket)");
        }

        /* Use default credentials */
        else {
            return new DefaultNTCredentials();
        }

        return promptForCredentials(null);
    }

    private Credentials getCredentials(final Repository repository)
            throws Exception {
        if (userCredentials == null) {
            final String username;
            final String password;

            if (repository == null) {
                final GitTFConfiguration config = GitTFConfiguration.loadFrom(getRepository());

                username = config.getUsername();
                password = config.getPassword();
            } else {
                username = GitTFConfiguration.getUsername(repository);
                password = GitTFConfiguration.getPassword(repository);
            }

            if (StringHelpers.isNullOrEmpty(username)) {
                userCredentials = getDefaultCredentials();

                if (userCredentials == null) {
                    throw new Exception("cancelled");
                }
            } else {
                userCredentials =
                        new UsernamePasswordCredentials(username, (StringHelpers.isNullOrEmpty(password))
                                ? promptForPassword() : password);
            }
        }

        return userCredentials;
    }

    protected TFSTeamProjectCollection getConnection()
            throws Exception {
        if (connection == null) {
            return getConnection(getServerConfiguration().getServerURI(), (Repository) null);
        }

        return connection;
    }

    protected TFSTeamProjectCollection getConnection(final URI serverURI, final Repository repository)
            throws Exception {
        if (connection == null) {
            AtomicReference<Credentials> credentials = new AtomicReference<Credentials>();
            credentials.set(getCredentials(repository));

            connection = getConnection(serverURI, credentials);

            userCredentials = credentials.get();
        }

        return connection;
    }

    private TFSTeamProjectCollection getConnection(final URI serverURI, final AtomicReference<Credentials> credentials)
            throws Exception {
        Check.notNull(serverURI, "serverURI");
        Check.notNull(credentials, "credentials");

        if (connection == null) {
            getProgressMonitor().displayMessage(Messages.getString("Command.ConnectingToTFS"));

            boolean authenticated = false, isHostedServer = false;
            int connectionTryCount = 0;
            while (!authenticated) {
                connectionTryCount++;

                connection = new TFSTeamProjectCollection(serverURI, credentials.get(), new GitTFConnectionAdvisor());

                try {
                    connection.ensureAuthenticated();
                    authenticated = true;
                } catch (TECoreException e) {
                    if (e.getCause() != null && e.getCause() instanceof EndpointNotFoundException) {
                        throw new Exception(Messages.formatString(
                                "Command.InvalidServerMissingCollectionFormat", serverURI.toString()), e);
                    }

                    if (connectionTryCount > 3) {
                        if (isHostedServer) {
                            throw new Exception(Messages.formatString(
                                    "Command.FailedToConnectToHostedFormat", serverURI.toString()), e);
                        }

                        throw e;
                    }

                    if (e instanceof ACSUnauthorizedException
                            || e instanceof TFSFederatedAuthException
                            || (e.getCause() != null && (e.getCause() instanceof AuthenticationException || e.getCause() instanceof UnauthorizedException))) {
                        if (connectionTryCount == 1) {
                            isHostedServer = e instanceof TFSFederatedAuthException;
                        }

                        Credentials newCredentials = promptForCredentials(connection.getCredentials());

                        if (newCredentials == null) {
                            throw e;
                        }

                        credentials.set(newCredentials);
                    } else {
                        throw e;
                    }
                }
            }
        }

        return connection;
    }

    protected VersionControlClient getVersionControlClient()
            throws Exception {
        if (versionControlClient == null) {
            versionControlClient = getConnection().getVersionControlClient();

            if (versionControlClient == null) {
                throw new Exception(Messages.getString("Command.ConnectionNotAvailable"));
            }

            versionControlClient.getEventEngine().addNonFatalErrorListener(new CommandNonFatalErrorListener());
        }

        return versionControlClient;
    }

    protected VersionControlService getVersionControlService()
            throws Exception {
        if (versionControlService == null) {
            versionControlService = new TfsVersionControlService(getVersionControlClient());
        }

        return versionControlService;
    }

    protected void verifyGitTfConfigured()
            throws Exception {
        GitTFConfiguration config = GitTFConfiguration.loadFrom(getRepository());

        if (config == null) {
            throw new Exception(Messages.getString("Command.GitTfNotConfigured"));
        }
    }

    protected void verifyGitTfConfiguredForRepositories(final String workingDirsSplitBySemicolon)
            throws Exception {
        final boolean isNotConfigured = getRepositories(workingDirsSplitBySemicolon).stream()
                .map(GitTFConfiguration::loadFrom)
                .anyMatch(Objects::isNull);

        if (isNotConfigured) {
            throw new Exception(Messages.getString("Command.GitTfNotConfigured"));
        }
    }

    protected void verifyMasterBranch()
            throws Exception {
        final Repository repository = getRepository();

        if (!RepositoryUtil.isEmptyRepository(repository)) {
            Ref master = repository.getRef(Constants.R_HEADS + Constants.MASTER);
            Ref head = repository.getRef(Constants.HEAD);
            Ref masterHeadRef = master != null ? master.getLeaf() : null;
            Ref currentHeadRef = head != null ? head.getLeaf() : null;

            if (masterHeadRef == null || !masterHeadRef.getName().equals(currentHeadRef.getName())) {
                throw new Exception(Messages.getString("Command.MasterNotCurrentBranch"));
            }
        }
    }

    protected void verifyNonBareRepo()
            throws Exception {
        Repository repository = getRepository();

        if (repository.isBare()) {
            throw new Exception(Messages.getString("Command.CommandNeedsNonBare"));
        }
    }

    private void verifyRepoSafeState(final Repository repository) throws Exception {

        final RepositoryState state = repository.getRepositoryState();

        switch (state) {
            case APPLY:
                throw new Exception(Messages.formatString(
                        "Command.OperationInProgressFormat", Messages.getString("Command.OperationApply")));

            case BISECTING:
                throw new Exception(Messages.formatString(
                        "Command.OperationInProgressFormat", Messages.getString("Command.OperationBisecting")));

            case MERGING:
            case MERGING_RESOLVED:
                throw new Exception(Messages.formatString(
                        "Command.OperationInProgressFormat", Messages.getString("Command.OperationMerge")));

            case REBASING:
            case REBASING_INTERACTIVE:
            case REBASING_MERGE:
            case REBASING_REBASING:
                throw new Exception(Messages.formatString(
                        "Command.OperationInProgressFormat", Messages.getString("Command.OperationRebase")));
        }
    }

    protected void verifyRepoSafeState() throws Exception {
        verifyRepoSafeState(getRepository());
    }

    protected void verifyReposSafeState(final String workingDirsSplitBySemicolon) throws Exception {
        final Set<Repository> repositories = getRepositories(workingDirsSplitBySemicolon);
        for (final Repository repository : repositories) {
            verifyRepoSafeState(repository);
        }
    }

    protected void verifyVersionSpec(VersionSpec versionSpec)
            throws Exception {
        if (versionSpec instanceof LabelVersionSpec || versionSpec instanceof WorkspaceVersionSpec) {
            throw new Exception(Messages.getString("Command.LabelAndWorkspaceVersionSpecsAreNotAllowed"));
        }
    }

    protected TaskProgressMonitor getProgressMonitor() {
        return new ConsoleTaskProgressMonitor(console);
    }

    public abstract int run()
            throws Exception;

    private class CommandNonFatalErrorListener
            implements NonFatalErrorListener {
        public void onNonFatalError(NonFatalErrorEvent nonFatal) {
            log.info(nonFatal.getMessage());
        }
    }

    // TODO Rework it and add more conditions to verify?
    private void verifyReposConsistency() {
        notNullOrEmpty(gitRepositories, "gitRepositories");
        final List<String> serverPaths = new ArrayList<>();
        final List<URI> serverURIs = new ArrayList<>();
        while (gitRepositories.iterator().hasNext()) {
            final Repository repository = gitRepositories.iterator().next();
            final GitTFConfiguration config = GitTFConfiguration.loadFrom(repository);
            serverPaths.add(config.getServerPath());
            if (!serverURIs.isEmpty() && !serverURIs.contains(config.getServerURI())) {
                throw new RuntimeException(Messages.getString("Command.GitUri.Mismatch"));
            }
            serverURIs.add(config.getServerURI());
        }
    }

    protected abstract boolean isMultiRepositories();
}
