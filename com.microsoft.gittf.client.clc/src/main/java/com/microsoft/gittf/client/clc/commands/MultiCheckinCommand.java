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

import com.microsoft.gittf.client.clc.Console.Verbosity;
import com.microsoft.gittf.client.clc.ExitCode;
import com.microsoft.gittf.client.clc.Main;
import com.microsoft.gittf.client.clc.Messages;
import com.microsoft.gittf.client.clc.arguments.*;
import com.microsoft.gittf.client.clc.commands.framework.CommandTaskExecutor;
import com.microsoft.gittf.client.clc.commands.framework.ConsoleOutputTaskHandler;
import com.microsoft.gittf.core.tasks.CheckinHeadCommitTask;
import com.microsoft.gittf.core.tasks.CheckinMultiRepositoriesHeadCommitsTask;
import com.microsoft.gittf.core.tasks.framework.Task;
import com.microsoft.gittf.core.tasks.framework.TaskCompletedHandler;
import com.microsoft.gittf.core.tasks.framework.TaskStatus;
import com.microsoft.gittf.core.tasks.pendDiff.RenameMode;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ActionDeniedBySubscriberException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.util.*;

public class MultiCheckinCommand
        extends PendingChangesCommand {
    public static final String COMMAND_NAME = "mcheckin";

    private static final Log log = LogFactory.getLog(MultiCheckinCommand.class);

    private static final CheckinTaskCompletedHandler checkinTaskCompletedHandler = new CheckinTaskCompletedHandler();

    private static Argument[] ARGUMENTS = new Argument[]
            {
                    new SwitchArgument("help", Messages.getString("Command.Argument.Help.HelpText")),

                    new ChoiceArgument(Messages.getString("Command.Argument.Display.HelpText"),
                            new SwitchArgument("quiet",
                                    'q',
                                    Messages.getString("Command.Argument.Quiet.HelpText")),

                            new SwitchArgument("verbose",
                                    Messages.getString("Command.Argument.Verbose.HelpText"))
                    ),

                    new ValueArgument("message",
                            'm',
                            Messages.getString("CheckinCommand.Argument.Message.ValueDescription"),
                            Messages.getString("CheckinCommand.Argument.Message.HelpText"),
                            ArgumentOptions.REQUIRED.combine(ArgumentOptions.VALUE_REQUIRED)),

                    new ValueArgument("dirs",
                            Messages.getString("MultiCheckinCommand.Argument.Dirs.ValueDescription"),
                            Messages.getString("MultiCheckinCommand.Argument.Dirs.HelpText"),
                            ArgumentOptions.REQUIRED.combine(ArgumentOptions.VALUE_REQUIRED)),

                    new ValueArgument("reviewer-code",
                            Messages.getString("CheckinCommand.Argument.ReviewerCode"),
                            Messages.getString("CheckinCommand.Argument.ReviewerCode.HelpText")
                    ),

                    new ChoiceArgument(Messages.getString("CheckinCommand.Argument.MetaDataChoice.HelpText"),
                            /* Users can specify one of --metadata or --no-metadata. */
                            new SwitchArgument("metadata",
                                    Messages.getString("CheckinCommand.Argument.MetaData.HelpText")),

                            new SwitchArgument("no-metadata",
                                    Messages.getString("CheckinCommand.Argument.NoMetaData.HelpText"))
                    ),

                    new ValueArgument("renamemode",
                            Messages.getString("PendingChangesCommand.Argument.RenameMode.ValueDescription"),
                            Messages.getString("PendingChangesCommand.Argument.RenameMode.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED),

                    new ChoiceArgument(Messages.getString("CheckinCommand.Argument.DepthChoice.HelpText"),
                            /* Users can specify one of --deep, --depth or --shallow. */
                            new SwitchArgument("deep",
                                    Messages.getString("CheckinCommand.Argument.Deep.HelpText")),

                            new SwitchArgument("shallow",
                                    Messages.getString("CheckinCommand.Argument.Shallow.HelpText"))
                    ),

                    new ChoiceArgument(Messages.getString("CheckinCommand.Argument.SquashAutoSquash.HelpText"),
                            /*
                             * User can specify one of --squash:[commit id],[commit id] or
                             * --autosquash
                             */
                            new ValueArgument("squash",
                                    Messages.getString("CheckinCommand.Argument.Squash.ValueDescription"),
                                    Messages.getString("CheckinCommand.Argument.Squash.HelpText"),
                                    ArgumentOptions.VALUE_REQUIRED.combine(ArgumentOptions.MULTIPLE)),

                            new SwitchArgument("autosquash",
                                    Messages.getString("CheckinCommand.Argument.AutoSquash.HelpText"))
                    ),

                    new ValueArgument("resolve",
                            Messages.getString("PendingChangesCommand.Argument.Resolve.ValueDescription"),
                            Messages.getString("PendingChangesCommand.Argument.Resolve.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED.combine(ArgumentOptions.MULTIPLE)),

                    new ValueArgument("associate",
                            Messages.getString("PendingChangesCommand.Argument.Associate.ValueDescription"),
                            Messages.getString("PendingChangesCommand.Argument.Associate.HelpText"),
                            ArgumentOptions.VALUE_REQUIRED.combine(ArgumentOptions.MULTIPLE)),

                    new SwitchArgument("mentions", Messages.getString("Command.Argument.Mentions.HelpText")),

                    new SwitchArgument("no-lock", Messages.getString("CheckinCommand.Argument.NoLock.HelpText")),

                    new SwitchArgument("preview", 'p', Messages.getString("CheckinCommand.Argument.Preview.HelpText")),

                    new ChoiceArgument(Messages.getString("CheckinCommand.Argument.GatedBuild.HelpText"),
                            /*
                             * User can specify one of --gated:[gatedbuildName] or --bypass
                             */
                            new SwitchArgument("bypass",
                                    Messages.getString("CheckinCommand.Argument.Bypass.HelpText")),

                            new ValueArgument("gated",
                                    'g',
                                    Messages.getString("CheckinCommand.Argument.Gated.ValueDescription"),
                                    Messages.getString("CheckinCommand.Argument.Gated.HelpText"),
                                    ArgumentOptions.VALUE_REQUIRED)),

                    new ChoiceArgument(
                            // no help text
                            new SwitchArgument("keep-author", Messages.getString("CheckinCommand.Argument.KeepAuthor.HelpText")),
                            new SwitchArgument("ignore-author", Messages.getString("CheckinCommand.Argument.IgnoreAuthor.HelpText"))
                    ),

                    new ValueArgument("user-map",
                            Messages.getString("CheckinCommand.Argument.UserMap.ValueDescription"),
                            Messages.getString("CheckinCommand.Argument.UserMap.HelpText"))

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
        return Messages.getString("CheckinCommand.HelpDescription");
    }

    @Override
    public int run()
            throws Exception {

        log.debug("Verifying configuration");
        final String workingDirsSplitBySemicolon = ((ValueArgument) getArguments().getArgument("dirs")).getValue();

        verifyGitTfConfiguredForRepositories(workingDirsSplitBySemicolon);
        verifyReposSafeState(workingDirsSplitBySemicolon);

        log.debug("Paring command parameters");

        boolean deep = getDeepFromArguments();

        boolean mentions = getArguments().contains("mentions");

        if (getArguments().contains("squash") && !getArguments().contains("deep"))
        {
            throw new Exception(Messages.getString("CheckinCommand.SquashOnlyAvailableWithDeep"));
        }

        final boolean noLock = getArguments().contains("no-lock");
        final boolean preview = getArguments().contains("preview");
        final boolean overrideGatedCheckin = getArguments().contains("bypass");
        final boolean autoSquashMultipleParents = getArguments().contains("autosquash");

        boolean includeMetaData = getIncludeMetaDataFromArguments();

        String message = getArguments().contains("message") ?
                ((ValueArgument) getArguments().getArgument("message")).getValue() : null;

        if (deep && message != null) {
            Main.printWarning(Messages.getString("CheckinCommand.MessageWillBeIgnoreBecauseDeepSpecified"));

            message = null;
        }

        final String buildDefinition = getArguments().contains("gated") ?
                ((ValueArgument) getArguments().getArgument("gated")).getValue() : null;

        final RenameMode renameMode = getRenameModeIfSpecified();

        boolean keepAuthor = getArguments().contains("keep-author") || !getArguments().contains("ignore-author");

        if (!deep && keepAuthor) {
            Main.printWarning("the check-in authors will be ignored because --deep is not specified");
            keepAuthor = false;
        }

        final String userMapPath = getArguments().contains("user-map") ?
                ((ValueArgument) getArguments().getArgument("user-map")).getValue() : null;

        final String codeReviewer = getArguments().contains("reviewer-code") ?
                ((ValueArgument) getArguments().getArgument("reviewer-code")).getValue() : null;

        final List<CheckinNoteFieldValue> checkinNoteFieldValues = new ArrayList<CheckinNoteFieldValue>();

        if (!StringUtils.isEmpty(codeReviewer)) {
            checkinNoteFieldValues.add(
                    new CheckinNoteFieldValue(CheckinNote.canonicalizeName("Code Reviewer"), codeReviewer));
        }

        final CheckinNote checkinNote =
                new CheckinNote(checkinNoteFieldValues.toArray(new CheckinNoteFieldValue[checkinNoteFieldValues.size()]));

        log.debug("Createing CheckinHeadCommitTask");

        final WorkItemClient witClient = mentions ? getConnection().getWorkItemClient() : null;
        final CheckinMultiRepositoriesHeadCommitsTask checkinTask =
                new CheckinMultiRepositoriesHeadCommitsTask(getRepositories(workingDirsSplitBySemicolon),
                        getVersionControlClient(), witClient);

        checkinTask.setWorkItemCheckinInfo(getWorkItemCheckinInfo());
        checkinTask.setDeep(deep);
        checkinTask.setLock(!noLock);
        checkinTask.setPreview(preview);
        checkinTask.setMentions(mentions);
        checkinTask.setOverrideGatedCheckin(overrideGatedCheckin);
        checkinTask.setSquashCommitIDs(getSquashCommitIDs(workingDirsSplitBySemicolon));
        checkinTask.setAutoSquash(autoSquashMultipleParents);
        checkinTask.setComment(message);
        checkinTask.setBuildDefinition(buildDefinition);
        checkinTask.setIncludeMetaDataInComment(includeMetaData);
        checkinTask.setRenameMode(renameMode);
        checkinTask.setKeepAuthor(keepAuthor);
        checkinTask.setUserMapPath(userMapPath);
        checkinTask.setCheckinNote(checkinNote);

        /*
         * Hook up a custom task executor that does not print gated errors to
         * standard error (we handle those specially.)
         */
        log.debug("Starting CheckinHeadCommitTask");
        final CommandTaskExecutor taskExecutor = new CommandTaskExecutor(getProgressMonitor());
        taskExecutor.removeTaskCompletedHandler(CommandTaskExecutor.CONSOLE_OUTPUT_TASK_HANDLER);
        taskExecutor.addTaskCompletedHandler(checkinTaskCompletedHandler);

        final TaskStatus checkinStatus = taskExecutor.execute(checkinTask);

        log.debug("CheckinHeadCommitTask finished");

        if (checkinStatus.isOK() && checkinStatus.getCode() == CheckinHeadCommitTask.ALREADY_UP_TO_DATE) {
            getConsole().getOutputStream(Verbosity.NORMAL).println(Messages.getString("CheckinCommand.AlreadyUpToDate"));
        }

        return checkinStatus.isOK() ? ExitCode.SUCCESS : ExitCode.FAILURE;
    }

    private Map<File, AbbreviatedObjectId[]> getSquashCommitIDs(final String workingDirsSplitBySemicolon)
            throws Exception {
        Set<Repository> repositories = getRepositories(workingDirsSplitBySemicolon);
        final Map<File, AbbreviatedObjectId[]> result = new HashMap<>();

        ObjectReader objReader = null;
        RevWalk revWalk = null;
        try {
            for (final Repository repository : repositories) {
                objReader = repository.newObjectReader();
                revWalk = new RevWalk(repository);

                Argument[] squashPrefixArgs = getArguments().getArguments("squash");

                if (squashPrefixArgs == null || squashPrefixArgs.length == 0) {
                    result.put(repository.getDirectory(), new AbbreviatedObjectId[0]);
                }

                AbbreviatedObjectId[] squashCommitIDs = new AbbreviatedObjectId[squashPrefixArgs.length];

                for (int i = 0; i < squashPrefixArgs.length; i++) {
                    squashCommitIDs[i] = AbbreviatedObjectId.fromString(((ValueArgument) squashPrefixArgs[i]).getValue());

                    Collection<ObjectId> candidateObjects = null;

                    try {
                        candidateObjects = objReader.resolve(squashCommitIDs[i]);
                    } catch (Exception e) {
                        /*
                         * commit id could not be resolved by git
                         */
                    }

                    if (candidateObjects == null || candidateObjects.size() == 0) {
                        throw new Exception(Messages.formatString(
                                "CheckinCommand.CommitIdAmbiguousFormat", squashCommitIDs[i].name()));
                    } else if (candidateObjects.size() > 1) {
                        throw new Exception(Messages.formatString(
                                "CheckinCommand.CommitIdAmbiguousFormat", squashCommitIDs[i].name()));
                    } else {
                        RevCommit revCommit = revWalk.parseCommit(candidateObjects.toArray(new ObjectId[1])[0]);

                        if (revCommit == null) {
                            throw new Exception(Messages.formatString(
                                    "CheckinCommand.CommitIdDoesNotExistFormat", squashCommitIDs[i].name()));
                        }
                    }
                }
                result.put(repository.getDirectory(), squashCommitIDs);
            }

            return result;
        } finally {
            if (objReader != null) {
                objReader.release();
            }

            if (revWalk != null) {
                revWalk.release();
            }
        }
    }

    private static class CheckinTaskCompletedHandler
            implements TaskCompletedHandler {
        private static final ConsoleOutputTaskHandler consoleOutputTaskHandler = new ConsoleOutputTaskHandler();

        public void onTaskCompleted(Task task, TaskStatus status) {
            if (status.getSeverity() == TaskStatus.ERROR
                    && status.getException() instanceof ActionDeniedBySubscriberException) {
                Main.printError(Messages.getString("CheckinCommand.GatedCheckinAborted"));
                Main.printError(status.getException().getLocalizedMessage(), false);
            } else {
                /* Delegate to console output handler */
                consoleOutputTaskHandler.onTaskCompleted(task, status);
            }
        }
    }

    @Override
    protected boolean isMultiRepositories() {
        return true;
    }
}
