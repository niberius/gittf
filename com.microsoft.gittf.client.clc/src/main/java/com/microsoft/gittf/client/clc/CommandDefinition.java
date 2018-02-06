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

package com.microsoft.gittf.client.clc;

import com.microsoft.gittf.client.clc.commands.framework.Command;
import com.microsoft.gittf.core.util.Check;

/**
 * Defines a git-tf command
 * 
 */
public class CommandDefinition
{
    private final String name;
    private final Class<? extends Command> type;
    private final String helpText;

    /**
     * Constructor
     * 
     * @param name
     *        - command name
     * @param type
     *        - command type
     * @param helpText
     *        - help text to be displayed for the command
     */
    public CommandDefinition(final String name, final Class<? extends Command> type, final String helpText)
    {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$
        Check.notNull(type, "type"); //$NON-NLS-1$
        Check.notNullOrEmpty(helpText, "helpText"); //$NON-NLS-1$

        this.name = name;
        this.type = type;
        this.helpText = helpText;
    }

    public String getName()
    {
        return name;
    }

    public Class<? extends Command> getType()
    {
        return type;
    }

    public String getHelpText()
    {
        return helpText;
    }
}
