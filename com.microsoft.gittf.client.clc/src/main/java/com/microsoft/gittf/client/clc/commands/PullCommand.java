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

package com.microsoft.gittf.client.clc.commands;

import com.microsoft.gittf.client.clc.ExitCode;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.arguments.*;
import com.microsoft.gittf.client.clc.commands.framework.Command;
import com.microsoft.gittf.client.clc.commands.framework.CommandTaskExecutor;
import com.microsoft.gittf.core.config.GitTFConfiguration;
import com.microsoft.gittf.core.tasks.PullTask;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.VersionSpecUtil;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import org.eclipse.jgit.merge.MergeStrategy;

public class PullCommand
        extends Command {
    public static final String COMMAND_NAME = "pull";

    private static final Argument[] ARGUMENTS = new Argument[]
            {
                    new SwitchArgument("help", Messages.getString("Command.Argument.Help.HelpText")),

                    new ChoiceArgument(Messages.getString("Command.Argument.Display.HelpText"),
                            new SwitchArgument("quiet",
                                    'q',
                                    Messages.getString("Command.Argument.Quiet.HelpText")),

                            new SwitchArgument("verbose",
                                    Messages.getString("Command.Argument.Verbose.HelpText"))
                    ),

                    new ValueArgument("version",
                            Messages.getString("Command.Argument.Version.ValueDescription"),
                            Messages.getString("PullCommand.Argument.Version.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new ChoiceArgument(Messages.getString("PullCommand.Argument.DepthChoice.HelpText"),

                            /* Users can specify one of --depth, --deep or --shallow. */
                            new SwitchArgument("deep",
                                    Messages.getString("PullCommand.Argument.Deep.HelpText")),

                            new SwitchArgument("shallow",
                                    Messages.getString("PullCommand.Argument.Shallow.HelpText"))
                    ),

                    new ChoiceArgument(Messages.getString("PullCommand.Argument.StrategyChoice.HelpText"),

                            /* Users can specify one of --depth, --deep or --shallow. */
                            new SwitchArgument("resolve",
                                    Messages.getString("PullCommand.Argument.Resolve.HelpText")),

                            new SwitchArgument("ours",
                                    Messages.getString("PullCommand.Argument.Ours.HelpText")),

                            new SwitchArgument("theirs",
                                    Messages.getString("PullCommand.Argument.Theirs.HelpText"))
                    ),

                    new SwitchArgument("rebase", Messages.getString("PullCommand.Argument.Rebase.HelpText")),

                    new SwitchArgument("force", Messages.getString("PullCommand.Argument.Force.HelpText")),

                    new SwitchArgument("mentions", Messages.getString("Command.Argument.Mentions.HelpText")),
            };

    @Override
    protected String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Argument[] getPossibleArguments() {
        return ARGUMENTS;
    }

    @Override
    public String getHelpDescription() {
        return Messages.getString("PullCommand.HelpDescription");
    }

    @Override
    public int run()
            throws Exception {
        if (getArguments().contains("rebase") && (getArguments().contains("ours") || getArguments().contains("theirs")))
        {
            throw new Exception(Messages.getString("PullCommand.RebaseNotSupportedWithOursAndTheirs"));
        }

        verifyGitTfConfigured();
        verifyNonBareRepo();
        verifyMasterBranch();
        verifyRepoSafeState();

        final VersionSpec versionSpec =
                getArguments().contains("version") ?
                        VersionSpecUtil.parseVersionSpec(((ValueArgument) getArguments().getArgument("version")).getValue()) : LatestVersionSpec.INSTANCE;

        verifyVersionSpec(versionSpec);

        boolean deep = GitTFConfiguration.loadFrom(getRepository()).getDeep();
        if (isDepthSpecified()) {
            deep = getDeepFromArguments();
        }

        final boolean mentions = getArguments().contains("mentions");
        if (mentions && !deep) {
            throw new Exception(Messages.getString("Command.MentionsOnlyAvailableWithDeep"));
        }

        final boolean force = getArguments().contains("force");

        final boolean rebase = getArguments().contains("rebase");

        final WorkItemClient witClient = mentions ? getConnection().getWorkItemClient() : null;
        final PullTask pullTask = new PullTask(getRepository(), getVersionControlService(), witClient);
        pullTask.setVersionSpec(versionSpec);
        pullTask.setDeep(deep);
        pullTask.setStrategy(getSpecifiedMergeStrategy());
        pullTask.setRebase(rebase);
        pullTask.setForce(force);

        final TaskStatus pullStatus = new CommandTaskExecutor(getProgressMonitor()).execute(pullTask);

        return pullStatus.isOK() ? ExitCode.SUCCESS : ExitCode.FAILURE;
    }

    private MergeStrategy getSpecifiedMergeStrategy() {
        if (getArguments().contains("ours"))
        {
            return MergeStrategy.OURS;
        } else if (getArguments().contains("theirs"))
        {
            return MergeStrategy.THEIRS;
        }

        return MergeStrategy.RESOLVE;
    }

    @Override
    protected boolean isMultiRepositories() {
        return false;
    }
}
