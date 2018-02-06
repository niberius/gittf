package com.microsoft.gittf.core.identity;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.gittf.core.GitTFConstants;
import com.microsoft.gittf.core.tasks.framework.TaskProgressMonitor;
import com.microsoft.gittf.core.util.Check;
import com.microsoft.gittf.core.util.StringUtil;
import com.microsoft.tfs.core.clients.webservices.IdentitySearchFactor;
import com.microsoft.tfs.util.StringHelpers;

public abstract class UserMap
{
    final Log log = LogFactory.getLog(UserMap.class.getName());

    private static final char BEGIN_LINE_COMMENT = '#';
    private static final char REST_OF_LINE_COMMENT = ';';
    private static final char SPLITTER = '=';
    private static final char SPACE = ' ';
    private static final char BEGIN_EMAIL = '<';
    private static final char END_EMAIL = '>';

    private static final int MIN_EMAIL_LENGTH = 3;
    private static final int MIN_USER_NAME_LENGTH = 1;

    private static final String INDENT_PREFIX = "    "; //$NON-NLS-1$

    private static final String MAPPED_USERS_SECTION_NAME = "[mapping]"; //$NON-NLS-1$
    private static final String UNKNOWN_USERS_SECTION_NAME = "[unknown]"; //$NON-NLS-1$
    private static final String DUPLICATE_USERS_SECTION_NAME = "[duplicates]"; //$NON-NLS-1$

    private static final String USER_MAP_FILE_HEADER =
        "The file provides mapping between Git users and known TFS user unique names. The Git user has to be represented as it appears in Git commits, including the user name and e-mail address. The TFS user has to be represented either as DOMAIN\\account (for on-premises TFS) or as Windows Live ID (for hosted TFS)."; //$NON-NLS-1$
    private static final String MAPPED_USERS_SECTION_HEADER =
        "The section contains mapping between Git users and TFS users. Add new mappings to this section as needed. Only this section is parsed when the file is used in a check-in command."; //$NON-NLS-1$
    private static final String UNKNOWN_USERS_SECTION_HEADER =
        "The section contains Git user names found in commits that cannot be mapped to TFS users automatically. You should provide mapping for these names or remove the --keep-author option from the check-in command. This section is not parsed when the file is used in a check-in command, you have to move resolved mapping to the " + MAPPED_USERS_SECTION_NAME + " section"; //$NON-NLS-1$ //$NON-NLS-2$
    private static final String DUPLICATE_USERS_SECTION_HEADER =
        "The section contains Git user names found in commits and for that automatic mapping process has found more than one corresponding TFS user. You should provide unique mapping for these names or remove the --keep-author option from the check-in command. This section is not parsed when the file is used in a check-in command, you have to move resolved mapping to the " + MAPPED_USERS_SECTION_NAME + " section"; //$NON-NLS-1$ //$NON-NLS-2$

    private static final int MAX_TITLE_LINE_LENGTH = 60;

    private final String userMapPath;

    private boolean inMappedUsersSection;

    private boolean loaded = false;
    private boolean changed = false;
    private boolean consistent = true;
    private boolean complete = false;

    private final Map<GitUser, List<TfsUser>> userMap = new HashMap<GitUser, List<TfsUser>>();

    private final Set<GitUser> gitUsers = new HashSet<GitUser>();

    protected UserMap(final String userMapPath)
    {
        this.userMapPath =
            !StringUtil.isNullOrEmpty(userMapPath) ? userMapPath : GitTFConstants.GIT_TF_DEFAULT_USER_MAP;
    }

    public boolean isLoaded()
    {
        return this.loaded;
    }

    public boolean isChanged()
    {
        return this.changed;
    }

    public boolean isConsistent()
    {
        return this.consistent;
    }

    public boolean isComplete()
    {
        return this.complete;
    }

    public boolean isOK()
    {
        return isConsistent() && isComplete();
    }

    public File getUserMapFile()
    {
        try
        {
            return (new File(userMapPath)).getCanonicalFile();
        }
        catch (final IOException e)
        {
            return null;
        }
    }

    public void load()
    {
        try
        {
            final File userMapFile = getUserMapFile();

            if (userMapFile.exists() && !userMapFile.isDirectory())
            {
                final List<String> userMapLines = readUserMapFile();

                inMappedUsersSection = false;

                for (final String line : userMapLines)
                {
                    parseUserLine(line);
                }

                loaded = true;
                changed = false;
            }
        }
        catch (final Exception e)
        {
        }
    }

    private void parseUserLine(final String line)
    {
        final String userInfo = stripOffComment(line);

        if (StringHelpers.isNullOrEmpty(userInfo))
        {
            return;
        }

        if (line.startsWith("[") && line.endsWith("]")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            inMappedUsersSection = line.equalsIgnoreCase(MAPPED_USERS_SECTION_NAME);
            return;
        }

        if (!inMappedUsersSection)
        {
            return;
        }

        final int splitterPos = userInfo.indexOf(SPLITTER);
        if (splitterPos < 0)
        {
            log.error(MessageFormat.format("User map file error: '{0}' not found.", SPLITTER)); //$NON-NLS-1$
            log.error(MessageFormat.format("       line ignored: {0}", line)); //$NON-NLS-1$
            return;
        }

        final String gitUserInfo = userInfo.substring(0, splitterPos).trim();
        if (StringHelpers.isNullOrEmpty(gitUserInfo))
        {
            log.error("User map file error: Git user information is missing."); //$NON-NLS-1$
            log.error(MessageFormat.format("       line ignored: {0}", line)); //$NON-NLS-1$
            return;
        }

        final String tfsUserInfo =
            splitterPos < userInfo.length() - 1 ? userInfo.substring(splitterPos + 1).trim() : null;
        if (StringHelpers.isNullOrEmpty(gitUserInfo))
        {
            log.error("User map file error: TFS user information is missing."); //$NON-NLS-1$
            log.error(MessageFormat.format("       line ignored: {0}", line)); //$NON-NLS-1$
            return;
        }

        final GitUser gitUser;
        try
        {
            gitUser = parseGitUser(gitUserInfo);
        }
        catch (final Exception e)
        {
            log.error(MessageFormat.format("User map file error: {0}", e.getMessage())); //$NON-NLS-1$
            log.error(MessageFormat.format("       line ignored: {0}", line)); //$NON-NLS-1$
            return;
        }

        final TfsUser tfsUser;
        try
        {
            tfsUser = parseTfsUser(tfsUserInfo);
        }
        catch (final Exception e)
        {
            log.error(MessageFormat.format("User map file error: {0}", e.getMessage())); //$NON-NLS-1$
            log.error(MessageFormat.format("       line ignored: {0}", line)); //$NON-NLS-1$
            return;
        }

        mapTfsUser(gitUser, tfsUser);
    }

    private GitUser parseGitUser(final String gitUserInfo)
    {
        final AtomicReference<String> name = new AtomicReference<String>();
        final AtomicReference<String> email = new AtomicReference<String>();

        splitGitUserInfo(gitUserInfo, name, email);

        return new GitUser(name.get(), email.get());
    }

    private void splitGitUserInfo(
        final String gitUserInfo,
        final AtomicReference<String> name,
        final AtomicReference<String> email)
    {
        final int beginEmail = gitUserInfo.indexOf(BEGIN_EMAIL);
        if (beginEmail < MIN_USER_NAME_LENGTH || beginEmail != gitUserInfo.lastIndexOf(BEGIN_EMAIL))
        {
            throw new RuntimeException("Incorrect Git user information"); //$NON-NLS-1$
        }

        final int endEmail = gitUserInfo.lastIndexOf(END_EMAIL);
        if (endEmail != gitUserInfo.length() - 1
            || endEmail != gitUserInfo.indexOf(END_EMAIL)
            || endEmail < beginEmail + MIN_EMAIL_LENGTH)
        {
            throw new RuntimeException("Incorrect Git user information"); //$NON-NLS-1$
        }

        name.set(gitUserInfo.substring(0, beginEmail).trim());
        email.set(gitUserInfo.substring(beginEmail + 1, endEmail).trim());
    }

    private TfsUser parseTfsUser(final String tfsUserInfo)
    {
        if (isValidDomainAccount(tfsUserInfo.replace('\\', '/')) || isValidWindowsLiveID(tfsUserInfo))
        {
            return new TfsUser(tfsUserInfo);
        }
        else
        {
            throw new RuntimeException("Incorrect TFS user information"); //$NON-NLS-1$
        }

    }

    private boolean isValidDomainAccount(final String domainAccount)
    {
        int k = domainAccount.indexOf('/');
        if (k >= 0
            && (k > domainAccount.length() - 2 || domainAccount.lastIndexOf('/') != k || domainAccount.indexOf('@') > 0))
        {
            throw new RuntimeException("Incorrect TFS user domain account"); //$NON-NLS-1$
        }

        return k > 0;
    }

    private boolean isValidWindowsLiveID(final String windowsLiveID)
    {
        int k = windowsLiveID.indexOf('@');
        if (k >= 0 && (k < 1 || k > windowsLiveID.length() - 2 || windowsLiveID.indexOf('/') > -1))
        {
            throw new RuntimeException("Incorrect TFS user Windows Live ID"); //$NON-NLS-1$
        }

        return k > 0;
    }

    private String stripOffComment(final String line)
    {
        final int beginLineCommentPos = line.indexOf(BEGIN_LINE_COMMENT);
        final int restOfLineCommentPos = line.indexOf(REST_OF_LINE_COMMENT);

        if (beginLineCommentPos < 0 && restOfLineCommentPos < 0)
        {
            return line.trim();
        }
        else
        {
            final int commentPos = min(max(beginLineCommentPos, 0), max(restOfLineCommentPos, 0));

            if (commentPos == 0)
            {
                return null;
            }
            else
            {
                return line.substring(0, commentPos - 1).trim();
            }
        }
    }

    public void save()
        throws Exception
    {
        final List<String> fileLines = new ArrayList<String>();

        addSectionHeader(fileLines, USER_MAP_FILE_HEADER);

        saveMappedUsers(fileLines);
        saveUnknownUsers(fileLines);
        saveDuplicateUsers(fileLines);

        final File userMapFile = getUserMapFile();

        if (!userMapFile.getParentFile().exists())
        {
            userMapFile.getParentFile().mkdirs();
        }
        else
        {
            final File bacFile = new File(userMapFile.getPath() + ".bak"); //$NON-NLS-1$

            if (bacFile.exists())
            {
                bacFile.delete();
            }

            userMapFile.renameTo(bacFile);
        }

        writeUserMapFile(fileLines);
    }

    private void saveMappedUsers(final List<String> fileLines)
    {
        addMappedUsersSection(fileLines);

        for (final GitUser gitUser : userMap.keySet())
        {
            final List<TfsUser> tfsUsers = userMap.get(gitUser);
            Check.isTrue(tfsUsers.size() > 0, "Unexpected empty TFS users list in the user map"); //$NON-NLS-1$

            if (tfsUsers.size() == 1)
            {
                addMappedUserRecord(fileLines, gitUser, tfsUsers.get(0));
            }
        }
    }

    private void saveUnknownUsers(final List<String> fileLines)
    {
        boolean firstUser = true;

        for (final GitUser gitUser : gitUsers)
        {
            if (!userMap.containsKey(gitUser))
            {
                if (firstUser)
                {
                    addUnknownUsersSection(fileLines);
                    firstUser = false;
                }

                addUnknownUserRecord(fileLines, gitUser);
            }
        }
    }

    private void saveDuplicateUsers(final List<String> fileLines)
    {
        boolean firstUser = true;

        for (final GitUser gitUser : userMap.keySet())
        {
            final List<TfsUser> tfsUsers = userMap.get(gitUser);
            Check.isTrue(tfsUsers.size() > 0, "Unexpected empty TFS users list in the user map"); //$NON-NLS-1$

            if (tfsUsers.size() > 1)
            {
                if (firstUser)
                {
                    addDuplicateUsersSection(fileLines);
                    firstUser = false;
                }

                addDuplicateUserRecords(fileLines, gitUser, tfsUsers);
            }
        }
    }

    protected abstract List<String> readUserMapFile()
        throws Exception;

    protected abstract void writeUserMapFile(final List<String> fileLines)
        throws Exception;

    protected abstract Map<String, List<TfsUser>> findTfsUsers(
        final TaskProgressMonitor progressMonitor,
        final List<String> searchValues,
        final IdentitySearchFactor searchFactor);

    protected Map<GitUser, List<TfsUser>> getUserMap()
    {
        return userMap;
    }

    public void addGitUsers()
    {
        /*
         * A convenience method, subclasses could override it to populate Git
         * user collection.
         * 
         * For example, TfsUserMap extracts Git users from commit data, while
         * unit tests may use different sources.
         * 
         * The goal is to keep the subclass constructor lightweight and separate
         * the initial loading from search and mapping.
         */
    }

    protected void addGitUser(final GitUser gitUser)
    {
        if (!gitUsers.contains(gitUser))
        {
            gitUsers.add(gitUser);
        }
    }

    private void mapTfsUsers(final GitUser gitUser, final List<TfsUser> tfsUsers)
    {
        for (final TfsUser tfsUser : tfsUsers)
        {
            mapTfsUser(gitUser, tfsUser);
        }
    }

    private void mapTfsUser(final GitUser gitUser, final TfsUser tfsUser)
    {
        if (!userMap.containsKey(gitUser))
        {
            userMap.put(gitUser, new ArrayList<TfsUser>());
        }

        final List<TfsUser> tfsUserList = userMap.get(gitUser);

        if (!tfsUserList.contains(tfsUser))
        {
            tfsUserList.add(tfsUser);
            changed = true;

            if (tfsUserList.size() > 1)
            {
                consistent = false;
            }
        }
    }

    protected Set<GitUser> getGitUsers()
    {
        return gitUsers;
    }

    public void check(final TaskProgressMonitor progressMonitor)
    {
        log.info("Check user mapping."); //$NON-NLS-1$

        final List<String> searchValues = getMappedTfsUserNames();
        final Map<String, List<TfsUser>> identityMapping =
            findTfsUsers(progressMonitor, searchValues, IdentitySearchFactor.GENERAL);

        for (final String tfsUserName : searchValues)
        {
            final List<TfsUser> foundIdentities = identityMapping.get(tfsUserName);

            if (foundIdentities == null)
            {
                log.warn(MessageFormat.format("No identity matching \"{0}\" found on the TFS server", tfsUserName)); //$NON-NLS-1$
            }
            else if (foundIdentities.size() > 1)
            {
                log.warn(MessageFormat.format("Multiple identities matching \"{0}\" found on the TFS server", //$NON-NLS-1$
                    tfsUserName));
            }
            else if (!tfsUserName.equalsIgnoreCase(foundIdentities.get(0).getName()))
            {
                log.warn(MessageFormat.format("Identity \"{0}\" has changed on the TFS server", tfsUserName)); //$NON-NLS-1$
            }
            else
            {
                continue;
            }

            removeMappingsToUser(tfsUserName);
        }
    }

    private void removeMappingsToUser(final String tfsUserName)
    {
        for (final Iterator<Entry<GitUser, List<TfsUser>>> iterator = userMap.entrySet().iterator(); iterator.hasNext();)
        {
            final Entry<GitUser, List<TfsUser>> entry = iterator.next();

            for (final TfsUser tfsUser : entry.getValue())
            {
                if (tfsUser.getName().equalsIgnoreCase(tfsUserName))
                {
                    entry.getValue().remove(tfsUser);
                    break;
                }
            }

            if (entry.getValue().size() == 0)
            {
                iterator.remove();
            }
        }
    }

    private List<String> getMappedTfsUserNames()
    {
        final Map<String, Object> uniqueUserNames = new HashMap<String, Object>();

        for (final List<TfsUser> tfsUsers : userMap.values())
        {
            for (final TfsUser user : tfsUsers)
            {
                final String userUniqueName = user.getName();
                if (!uniqueUserNames.containsKey(userUniqueName))
                {
                    uniqueUserNames.put(userUniqueName, null);
                }
            }
        }

        final List<String> userNames = new ArrayList<String>();
        userNames.addAll(uniqueUserNames.keySet());

        return userNames;
    }

    public void searchTfsUsers(final TaskProgressMonitor progressMonitor)
    {

        // We search TFS user identities in the following order:
        //
        // 0. By a Git user e-mail address as a TFS user name.
        // 1. By a Git user e-mail address as a TFS user e-mail.
        // 2. By a Git user name address as a TFS user name.

        for (int i = 0; i < 3; i++)
        {
            List<String> searchValues = null;
            IdentitySearchFactor searchFactor = null;

            switch (i)
            {
                case 0:
                    searchValues = getNotMappedUserEmails();
                    searchFactor = IdentitySearchFactor.GENERAL;
                    break;
                case 1:
                    searchValues = getNotMappedUserEmails();
                    searchFactor = IdentitySearchFactor.MAIL_ADDRESS;
                    break;
                case 2:
                    searchValues = getNotMappedUserNames();
                    searchFactor = IdentitySearchFactor.GENERAL;
                    break;
            }

            if (searchValues.size() == 0)
            {
                complete = true;
            }

            final Map<String, List<TfsUser>> newMapping = findTfsUsers(progressMonitor, searchValues, searchFactor);

            if (newMapping.size() > 0)
            {
                for (final GitUser gitUser : getNotMappedUsers())
                {
                    final String key = i < 2 ? gitUser.getEmail() : gitUser.getName();

                    if (newMapping.containsKey(key))
                    {
                        final List<TfsUser> mappedUsers = newMapping.get(key);
                        mapTfsUsers(gitUser, mappedUsers);
                    }
                }
            }
        }
    }

    public TfsUser getTfsUser(final GitUser gitUser)
    {
        if (userMap.containsKey(gitUser))
        {
            List<TfsUser> mappedUsers = userMap.get(gitUser);

            if (mappedUsers.size() == 1)
            {
                return mappedUsers.get(0);
            }
            else
            {
                throw new RuntimeException(MessageFormat.format(
                    "The Git user {0} <{1}> is mapped to more than one TFS identity", gitUser)); //$NON-NLS-1$
            }
        }
        else
        {
            throw new RuntimeException(MessageFormat.format(
                "The Git user {0} <{1}> is not mapped to a TFS identity", gitUser)); //$NON-NLS-1$
        }
    }

    private List<String> getNotMappedUserNames()
    {
        final List<String> userNames = new ArrayList<String>();

        for (final GitUser gitUser : gitUsers)
        {
            if (!userMap.containsKey(gitUser))
            {
                userNames.add(gitUser.getName());
            }
        }

        return userNames;
    }

    private List<String> getNotMappedUserEmails()
    {
        final List<String> userEmails = new ArrayList<String>();

        for (final GitUser gitUser : gitUsers)
        {
            if (!userMap.containsKey(gitUser))
            {
                userEmails.add(gitUser.getEmail());
            }
        }

        return userEmails;
    }

    private List<GitUser> getNotMappedUsers()
    {
        final List<GitUser> notMappedUsers = new ArrayList<GitUser>();

        for (final GitUser gitUser : gitUsers)
        {
            if (!userMap.containsKey(gitUser))
            {
                notMappedUsers.add(gitUser);
            }
        }

        return notMappedUsers;
    }

    private void addUnknownUserRecord(final List<String> fileLines, final GitUser gitUser)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(INDENT_PREFIX);
        sb.append(gitUser);
        sb.append(" = "); //$NON-NLS-1$

        fileLines.add(sb.toString());
    }

    private void addDuplicateUserRecords(
        final List<String> fileLines,
        final GitUser gitUser,
        final List<TfsUser> tfsUsers)
    {
        for (final TfsUser tfsUser : tfsUsers)
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(INDENT_PREFIX);
            sb.append(gitUser);
            sb.append(" = "); //$NON-NLS-1$
            sb.append(tfsUser.getName());

            fileLines.add(sb.toString());
        }

    }

    private void addMappedUserRecord(final List<String> fileLines, final GitUser gitUser, final TfsUser tfsUser)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(INDENT_PREFIX);
        sb.append(gitUser);
        sb.append(" = "); //$NON-NLS-1$
        sb.append(tfsUser.getName());

        fileLines.add(sb.toString());
    }

    private void addSectionHeader(final List<String> fileLines, final String sectionHeader)
    {
        final String[] words = sectionHeader.split(" "); //$NON-NLS-1$
        final StringBuilder sb = new StringBuilder(100);

        sb.append(BEGIN_LINE_COMMENT);

        for (int k = 0; k < words.length; k++)
        {
            final String word = words[k];

            if (word.length() > 0)
            {
                if (sb.length() > MAX_TITLE_LINE_LENGTH)
                {
                    fileLines.add(sb.toString());
                    sb.delete(0, sb.length());
                    sb.append(BEGIN_LINE_COMMENT);
                }

                sb.append(SPACE);
                sb.append(word);
            }
        }

        if (sb.length() > 1)
        {
            fileLines.add(sb.toString());
        }
    }

    private void addMappedUsersSection(final List<String> fileLines)
    {
        fileLines.add(""); //$NON-NLS-1$
        addSectionHeader(fileLines, MAPPED_USERS_SECTION_HEADER);
        fileLines.add(""); //$NON-NLS-1$
        fileLines.add(MAPPED_USERS_SECTION_NAME);
    }

    private void addUnknownUsersSection(final List<String> fileLines)
    {
        fileLines.add(""); //$NON-NLS-1$
        addSectionHeader(fileLines, UNKNOWN_USERS_SECTION_HEADER);
        fileLines.add(""); //$NON-NLS-1$
        fileLines.add(UNKNOWN_USERS_SECTION_NAME);
    }

    private void addDuplicateUsersSection(final List<String> fileLines)
    {
        fileLines.add(""); //$NON-NLS-1$
        addSectionHeader(fileLines, DUPLICATE_USERS_SECTION_HEADER);
        fileLines.add(""); //$NON-NLS-1$
        fileLines.add(DUPLICATE_USERS_SECTION_NAME);
    }
}
