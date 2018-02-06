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

package com.microsoft.gittf.core.util;

/**
 * Repository path utility functions.
 * 
 */
public final class RepositoryPath
{
    /**
     * Constructor
     */
    private RepositoryPath()
    {

    }

    /**
     * The preferred separator character.
     */
    public static final char PREFERRED_SEPARATOR_CHARACTER = '/';
    public static final String PREFERRED_SEPARATOR_STRING = "/"; //$NON-NLS-1$

    /**
     * Allowed path separator characters in repository paths. All characters are
     * equivalent. Forward slash ('/') is the preferred character.
     */
    public static final char[] SEPARATOR_CHARACTERS =
    {
        '/', '\\'
    };

    /**
     * Gets just the folder part of the given repository path, which is all of
     * the string up to the last part (the file part). If the given path
     * describes a folder but does not end in a separator, the last folder is
     * discarded.
     * 
     * @param repositoryPath
     *        the repository path of which to return the folder part (must not
     *        be <code>null</code>)
     * @return a repository path with only the folder part of the given path,
     *         ending in a separator character.
     */
    public static String getParent(String repositoryPath)
    {
        Check.notNull(repositoryPath, "repositoryPath"); //$NON-NLS-1$

        int largestIndex = -1;
        for (int i = 0; i < RepositoryPath.SEPARATOR_CHARACTERS.length; i++)
        {
            largestIndex = Math.max(largestIndex, repositoryPath.lastIndexOf(RepositoryPath.SEPARATOR_CHARACTERS[i]));
        }

        if (largestIndex != -1)
        {
            return repositoryPath.substring(0, largestIndex);

        }

        return ""; //$NON-NLS-1$
    }

    /**
     * Determines if the specified parent is an ancestor of the specified child.
     * 
     * @param child
     * @param ancestor
     * @return true if child is a decendant of parent
     */
    public static boolean isAncestor(String child, String ancestor)
    {
        if (child.length() <= ancestor.length())
        {
            return false;
        }

        String currentParent = getParent(child);
        while (currentParent != null && currentParent.length() > 0)
        {
            if (currentParent.equals(ancestor))
            {
                return true;
            }

            currentParent = getParent(currentParent);
        }

        return false;
    }

    /**
     * Gets just the file part of the given server path, which is all of the
     * string after the last path part. If there are no separators, the
     * entire string is returned. If the string ends in a separator, an empty
     * string is returned.
     * 
     * @param repositoryPath
     *        the repository path from which to parse the file part (must not be
     *        <code>null</code>)
     * @return the file name at the end of the given repository path, or the
     *         given path if no separator characters were found, or an empty
     *         string if the given path ends with a separator.
     */
    public static String getFileName(final String repositoryPath)
    {
        Check.notNull(repositoryPath, "repositoryPath"); //$NON-NLS-1$

        int largestIndex = -1;
        for (int i = 0; i < RepositoryPath.SEPARATOR_CHARACTERS.length; i++)
        {
            largestIndex = Math.max(largestIndex, repositoryPath.lastIndexOf(RepositoryPath.SEPARATOR_CHARACTERS[i]));
        }

        if (largestIndex == -1)
        {
            return repositoryPath;
        }

        /*
         * Add 1 to return the part after the sep, unless that would be longer
         * than the string ("$/Project/folder/" would be that case).
         */
        if (largestIndex + 1 < repositoryPath.length())
        {
            return repositoryPath.substring(largestIndex + 1);
        }
        else
        {
            return ""; //$NON-NLS-1$
        }
    }

    /**
     * Returns the depth of the item described by path, where the root folder is
     * depth 0, team projects are at depth 1, and so on.
     * 
     * @param repositoryPath
     *        the repository path to test (must not be <code>null</code>)
     * @return the depth from root, where root is 0, team projects are 1, etc.
     */
    public static int getFolderDepth(final String repositoryPath)
    {
        return RepositoryPath.getFolderDepth(repositoryPath, Integer.MAX_VALUE);
    }

    /**
     * Returns the depth of the item described by path, where the root folder is
     * depth 0, team projects are at depth 1, "$/Project/File" is 2, and so on.
     * 
     * @param repositoryPath
     *        the repository path to test (must not be <code>null</code>)
     * @param maxDepth
     *        the maximum depth to search.
     * @return the depth from root, where root is 0, team projects are 1, etc.
     */
    public static int getFolderDepth(final String repositoryPath, final int maxDepth)
    {
        Check.notNull(repositoryPath, "repositoryPath"); //$NON-NLS-1$

        int depth = 0;

        for (int i = repositoryPath.indexOf(PREFERRED_SEPARATOR_STRING); i != -1 && maxDepth > depth; i =
            repositoryPath.indexOf(PREFERRED_SEPARATOR_STRING, i + 1))
        {
            depth++;
        }

        return depth;
    }
}
