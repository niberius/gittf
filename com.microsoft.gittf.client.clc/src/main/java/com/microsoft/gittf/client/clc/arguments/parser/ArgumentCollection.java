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

package com.microsoft.gittf.client.clc.arguments.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.gittf.client.clc.arguments.Argument;
import com.microsoft.gittf.client.clc.arguments.ArgumentOptions;
import com.microsoft.gittf.core.util.Check;

public final class ArgumentCollection
{
    private final List<Argument> argumentList = new ArrayList<Argument>();
    private final Map<String, List<Argument>> argumentsByName = new HashMap<String, List<Argument>>();
    private final List<String> unknownArguments = new ArrayList<String>();

    public ArgumentCollection()
    {
    }

    public void add(Argument argument)
    {
        Check.notNull(argument, "argument"); //$NON-NLS-1$

        List<Argument> argumentsForName = argumentsByName.get(argument.getName());

        if (argumentsForName == null)
        {
            argumentsForName = new ArrayList<Argument>();
            argumentsByName.put(argument.getName(), argumentsForName);
        }

        /*
         * If this key cannot have multiple values, then clear the list of
         * existing values.
         */
        if (!argument.getOptions().contains(ArgumentOptions.MULTIPLE))
        {
            for (Argument remove : argumentsForName)
            {
                argumentList.remove(remove);
            }

            argumentsForName.clear();
        }

        argumentsForName.add(argument);
        argumentList.add(argument);
    }

    public List<Argument> getArguments()
    {
        return argumentList;
    }

    public boolean contains(String name)
    {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        return argumentsByName.containsKey(name);
    }

    public Argument getArgument(String name)
    {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        List<Argument> argumentsForName = argumentsByName.get(name);

        if (argumentsForName == null || argumentsForName.size() == 0)
        {
            return null;
        }

        return argumentsForName.get(0);
    }

    public Argument[] getArguments(String name)
    {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        List<Argument> argumentsForName = argumentsByName.get(name);

        if (argumentsForName == null)
        {
            return new Argument[0];
        }

        return argumentsForName.toArray(new Argument[argumentsForName.size()]);
    }

    public void addUnknownArgument(String argument)
    {
        Check.notNullOrEmpty(argument, "argument"); //$NON-NLS-1$

        unknownArguments.add(argument);
    }

    public String[] getUnknownArguments()
    {
        return unknownArguments.toArray(new String[unknownArguments.size()]);
    }

    @Override
    public int hashCode()
    {
        int hashCode = 31 * argumentList.hashCode();
        hashCode = 31 * unknownArguments.hashCode();

        return hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof ArgumentCollection))
        {
            return false;
        }

        ArgumentCollection other = (ArgumentCollection) o;

        if (!argumentList.equals(other.argumentList))
        {
            return false;
        }

        if (!unknownArguments.equals(other.unknownArguments))
        {
            return false;
        }

        return true;
    }
}
