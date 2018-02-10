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
import com.microsoft.gittf.client.clc.commands.framework.CommandTaskExecutor;
import com.microsoft.gittf.core.tasks.ShelveMultiRepositoriesDifferencesTask;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.util.CommitUtil;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MultiShelveCommand
        extends PendingChangesCommand {
    public static final String COMMAND_NAME = "mshelve";

    private static Argument[] ARGUMENTS = new Argument[]
            {
                    new SwitchArgument("help",
                            Messages.getString("Command.Argument.Help.HelpText"),
                            ArgumentOptions.SUPPRESS_REQUIREMENTS),

                    new ChoiceArgument(Messages.getString("Command.Argument.Display.HelpText"),
                            new SwitchArgument("quiet",
                                    'q',
                                    Messages.getString("Command.Argument.Quiet.HelpText")),

                            new SwitchArgument("verbose",
                                    Messages.getString("Command.Argument.Verbose.HelpText"))
                    ),

                    new SwitchArgument("replace", Messages.getString("ShelveCommand.Argument.Replace.HelpText")),

                    new ValueArgument("renamemode",
                            Messages.getString("PendingChangesCommand.Argument.RenameMode.ValueDescription"),
                            Messages.getString("PendingChangesCommand.Argument.RenameMode.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new ValueArgument("message",
                            'm',
                            Messages.getString("ShelveCommand.Argument.Message.ValueDescription"),
                            Messages.getString("ShelveCommand.Argument.Message.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new ValueArgument("git-dirs",
                            Messages.getString("ShelveCommand.Argument.GitDirs.ValueDescription"),
                            Messages.getString("ShelveCommand.Argument.GitDirs.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED.combine(ArgumentOptions.REQUIRED)),

                    new ValueArgument("resolve",
                            Messages.getString("PendingChangesCommand.Argument.Resolve.ValueDescription"),
                            Messages.getString("PendingChangesCommand.Argument.Resolve.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED.combine(ArgumentOptions.MULTIPLE)),

                    new ValueArgument("associate",
                            Messages.getString("PendingChangesCommand.Argument.Associate.ValueDescription"),
                            Messages.getString("PendingChangesCommand.Argument.Associate.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED.combine(ArgumentOptions.MULTIPLE)),

                    new FreeArgument("name", Messages.getString("ShelveCommand.Argument.Name.HelpText"), ArgumentOptions.REQUIRED),

                    new FreeArgument("ref-spec", Messages.getString("ShelveCommand.Argument.RefSpec.HelpText")),
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
        return Messages.getString("ShelveCommand.HelpDescription");
    }

    @Override
    public int run()
            throws Exception {
        final String gitDirsSplitBySemicolon = ((ValueArgument) getArguments().getArgument("git-dirs")).getValue();
        verifyGitTfConfiguredForRepositories(gitDirsSplitBySemicolon);
        verifyReposSafeState(gitDirsSplitBySemicolon);

        final Set<Repository> repositories = getRepositories(gitDirsSplitBySemicolon);

        final String shelvesetName = ((FreeArgument) getArguments().getArgument("name")).getValue();

        final String message = getArguments().contains("message") ?
                ((ValueArgument) getArguments().getArgument("message")).getValue() : null;

        final String refString =
                getArguments().contains("ref-spec") ? ((FreeArgument) getArguments().getArgument("ref-spec")).getValue()
                        : null;

        final Map<File, ObjectId> gitDirWithShelveCommitID = new HashMap<>();
        for (final Repository repository : repositories) {
            final ObjectId commitToShelve =
                    refString == null ? CommitUtil.getCurrentBranchHeadCommitID(repository) : CommitUtil.getRefNameCommitID(
                            repository,
                            refString);

            if (commitToShelve == null
                    || ObjectId.zeroId().equals(commitToShelve)
                    || !CommitUtil.isValidCommitId(repository, commitToShelve)) {
                throw new Exception(Messages.formatString("ShelveCommnad.InvalidRefSpecFormat", refString));
            }
            gitDirWithShelveCommitID.put(repository.getDirectory(), commitToShelve);
        }
        final ShelveMultiRepositoriesDifferencesTask shelveTask =
                new ShelveMultiRepositoriesDifferencesTask(repositories, gitDirWithShelveCommitID,
                        getVersionControlClient(), shelvesetName);

        shelveTask.setMessage(message);
        shelveTask.setWorkItemCheckinInfo(getWorkItemCheckinInfo());
        shelveTask.setReplaceExistingShelveset(getArguments().contains("replace"));
        shelveTask.setRenameMode(getRenameModeIfSpecified());

        final TaskStatus shelveStatus = new CommandTaskExecutor(getProgressMonitor()).execute(shelveTask);

        return shelveStatus.isOK() ? ExitCode.SUCCESS : ExitCode.FAILURE;
    }
}
