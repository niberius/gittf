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

package com.microsoft.gittf.client.clc;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Extracts the git-tf product information from the version file
 */
public class ProductInformation {
    /**
     * If you change this resource, make sure to update the build script that
     * writes the version info into the file.
     */
    private static final String VERSION_PROPERTIES_RESOURCE = "/git-tf-version.properties";
    private static final String productName = "git-tf";

    private static final String developVersion = "DEVELOP";
    private static final String snapshotVersion = "SNAPSHOT";

    private static String major = "";
    private static String minor = "";
    private static String service = "";
    private static String build = "";
    private static String buildNumber = "";

    private static Throwable loadException;

    static {
        InputStream in = ProductInformation.class.getResourceAsStream(VERSION_PROPERTIES_RESOURCE);

        if (in != null) {
            try {
                Properties props = new Properties();
                try {
                    props.load(in);
                    buildNumber = props.getProperty("buildNumber");
                    major = props.getProperty("version.major");
                    minor = props.getProperty("version.minor");
                    service = props.getProperty("version.service");
                    build = props.containsKey("version.build") ? props.getProperty("version.build") : developVersion;

                    if (build.equals(snapshotVersion) || build.equals(developVersion)) {
                        buildNumber = MessageFormat.format("{0}.{1}.{2}.{3}", major, minor, service, developVersion);
                    }
                } catch (IOException e) {
                    loadException = e;
                }
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    loadException = e;
                }
            }
        } else {
            loadException =
                    new Exception(MessageFormat.format(
                            Messages.getString("ProductInformation.UnableToLoadVersionPropertiesResourceFormat"),
                            VERSION_PROPERTIES_RESOURCE));
        }
    }

    /**
     * Constructor
     */
    private ProductInformation() {
    }

    public static String getProductName() {
        return productName;
    }

    public static String getMajorVersion() {
        if (loadException != null) {
            throw new RuntimeException(loadException);
        }
        return major;
    }

    public static String getMinorVersion() {
        if (loadException != null) {
            throw new RuntimeException(loadException);
        }
        return minor;
    }

    public static String getServiceVersion() {
        if (loadException != null) {
            throw new RuntimeException(loadException);
        }
        return service;
    }

    public static String getBuildVersion() {
        if (loadException != null) {
            throw new RuntimeException(loadException);
        }
        return build;
    }

    public static String getBuildNumber() {
        if (loadException != null) {
            throw new RuntimeException(loadException);
        }
        return buildNumber;
    }
}
