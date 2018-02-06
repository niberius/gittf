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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;

public final class ObjectIdUtil
{
    private static final int ABBREVIATED_LENGTH = 7;

    private static final Log log = LogFactory.getLog(ObjectIdUtil.class);

    private ObjectIdUtil()
    {
    }

    /**
     * Abbreviates an object id to seven char representation
     * 
     * @param objectID
     *        the object id to abbreviate
     * 
     * @return String representation of the abbreviated object id
     */
    public static final String abbreviate(final ObjectId objectID)
    {
        return abbreviate(null, objectID);
    }

    /**
     * Abbreviates an object id to minimum of seven char representation that is
     * unique to the repository
     * 
     * @param objectID
     *        the object id to abbreviate
     * 
     * @return String representation of the abbreviated object id
     */
    public static final String abbreviate(final Repository repository, final ObjectId objectID)
    {
        Check.notNull(objectID, "objectID"); //$NON-NLS-1$

        if (repository != null)
        {
            ObjectReader objReader = repository.getObjectDatabase().newReader();

            try
            {
                return objReader.abbreviate(objectID, ABBREVIATED_LENGTH).name();
            }
            catch (IOException e)
            {
                log.warn("Could not read object from object database", e); //$NON-NLS-1$
            }
            finally
            {
                if (objReader != null)
                {
                    objReader.release();
                }
            }
        }

        return objectID.getName().substring(0, ABBREVIATED_LENGTH);
    }
}
