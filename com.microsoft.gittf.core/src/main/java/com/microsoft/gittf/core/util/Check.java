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

package com.microsoft.gittf.core.util;

import com.microsoft.gittf.core.Messages;

import java.util.Collection;
import java.util.Map;

public final class Check {
    private Check() {
    }

    /**
     * Throws NullPointerException if the given object is null.
     *
     * @param o            the object to check.
     * @param variableName the name of the variable being checked (may be null).
     */
    public static void notNull(final Object o, final String variableName) {
        if (o == null) {
            throwForNull(variableName);
        }
    }

    /**
     * Throws IllegalArgumentException if the given string is not null and its
     * length is 0.
     *
     * @param string       the string to check.
     * @param variableName the name of the variable being checked (may be null).
     */
    public static void notEmpty(final String string, final String variableName) {
        if (string != null && string.length() == 0) {
            throwForEmpty(variableName);
        }
    }

    /**
     * Throws NullPointerException if the given string is null,
     * IllegalArgumentException if the given string's length is 0.
     *
     * @param string       the string to check.
     * @param variableName the name of the variable being checked (may be null).
     */
    public static void notNullOrEmpty(final String string, final String variableName) {
        if (string == null) {
            throwForNull(variableName);
        }
        if (string.length() == 0) {
            throwForEmpty(variableName);
        }
    }

    /**
     * Throws NullPointerException if the given array is null,
     * IllegalArgumentException if the given array's length is 0.
     *
     * @param array        the array to check.
     * @param variableName the name of the variable being checked (may be null).
     */
    public static void notNullOrEmpty(final Object[] array, final String variableName) {
        if (array == null) {
            throwForNull(variableName);
        }
        if (array.length == 0) {
            throwForEmpty(variableName);
        }
    }

    /**
     * Throws NullPointerException if the given collection is null,
     * IllegalArgumentException if the given collection size is 0.
     *
     * @param collection   the collection to check.
     * @param variableName the name of the variable being checked (may be null).
     */
    public static void notNullOrEmpty(final Collection collection, final String variableName) {
        if (collection == null) {
            throwForNull(variableName);
        }
        if (collection.isEmpty()) {
            throwForEmpty(variableName);
        }
    }

    public static void notNullOrEmpty(final Map map, final String variableName) {
        if (map == null) {
            throwForNull(variableName);
        }
        if (map.isEmpty()) {
            throwForEmpty(variableName);
        }
        for (Object key : map.keySet()) {
            if (map.get(key) == null) {
                throwForNull(variableName);
            }
        }
    }

    /**
     * Throws IllegalArgumentException if the given condition is false.
     *
     * @param condition the condition to test.
     * @param message   the message to put in the exception (may be null for a generic
     *                  message).
     */
    public static void isTrue(final boolean condition, final String message) {
        if (condition == false) {
            throwForFalse(message);
        }
    }

    /**
     * Throws a NullPointerException formatted with a string for the given
     * variable name.
     *
     * @param variableName the name of the variable being checked (may be null for a generic
     *                     message).
     */
    private static void throwForNull(String variableName) {
        // This is the best we can do.
        if (variableName == null) {
            variableName = Messages.getString("Check.Argument");
        }

        throw new NullPointerException(Messages.formatString("Check.ArgumentNotEmptyFormat", variableName));
    }

    /**
     * Throws an IllegalArgumentException formatted with a string for the given
     * variable name.
     *
     * @param variableName the name of the variable being checked (may be null for a generic
     *                     message).
     */
    private static void throwForEmpty(String variableName) {
        // This is the best we can do.
        if (variableName == null) {
            variableName = Messages.getString("Check.Argument");
        }

        throw new IllegalArgumentException(Messages.formatString("Check.ArgumentNotEmptyFormat", variableName));
    }

    /**
     * Throws an IllegalArgumentException formatted with a message for when a
     * boolean condition is false.
     *
     * @param message the message to be included in the exception (may be null for a
     *                generic message).
     */
    private static void throwForFalse(String message) {
        // This is the best we can do.
        if (message == null) {
            message = Messages.getString("Check.ConditionMustNotBeFalse");
        }

        throw new IllegalArgumentException(message);
    }
}
