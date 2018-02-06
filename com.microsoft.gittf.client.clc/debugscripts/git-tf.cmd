@echo off
rem
rem  ------------------------------------------------------------------------------------------------
rem  Copyright (c) Microsoft Corporation
rem  All rights reserved.
rem
rem  MIT License:
rem
rem  Permission is hereby granted, free of charge, to any person obtaining
rem  a copy of this software and associated documentation files (the
rem  "Software"), to deal in the Software without restriction, including
rem  without limitation the rights to use, copy, modify, merge, publish,
rem  distribute, sublicense, and/or sell copies of the Software, and to
rem  permit persons to whom the Software is furnished to do so, subject to
rem  the following conditions:
rem
rem  The above copyright notice and this permission notice shall be
rem  included in all copies or substantial portions of the Software.
rem
rem  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
rem  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
rem  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
rem  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
rem  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
rem  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
rem  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
rem  ------------------------------------------------------------------------------------------------
rem
rem  Script for debugging git-tf while developing in Eclipse.  Eclipse builds the classes into
rem  ../bin and the library dependencies are in ../libs
rem

setlocal

set LAUNCHER_CLASS=com.microsoft.gittf.client.clc.Main

setlocal ENABLEEXTENSIONS
setlocal ENABLEDELAYEDEXPANSION

set BASE_DIRECTORY=%~dp0

rem  List of dependent projects.
set DEP_PROJECTS=com.microsoft.gittf.client.clc com.microsoft.gittf.core

rem  List of dependent jars from maven
set DEP_MAVEN=org\eclipse\jgit\org.eclipse.jgit

rem  Add all the dependent project "target\classes" dirs to the classpath
for %%a in (%DEP_PROJECTS%) do (for /d %%u in (%BASE_DIRECTORY%..\..\%%a\target\classes) do set GITTF_CLASSPATH=!GITTF_CLASSPATH!;%%u)

rem  Include all the "lib" and "libs" directories
set DEP_PROJECTS_LIBDIRS=
for %%i in (%DEP_PROJECTS%) do set DEP_PROJECTS_LIBDIRS=!DEP_PROJECTS_LIBDIRS!;%BASE_DIRECTORY%..\..\%%i\lib;%BASE_DIRECTORY%..\..\%%i\libs

rem  Add each JAR in each libdir to the classpath
for %%a in (%DEP_PROJECTS_LIBDIRS%) do (for %%u in (%%a\*.jar) do set GITTF_CLASSPATH=!GITTF_CLASSPATH!;%%u)

rem  Find the newest (ie, last sorted) version of this maven dependency in the
rem  maven repository directory.  Add all the JARs in that directory to the
rem  classpath.
set M2_REPOSITORY=%USERPROFILE%\.m2\repository
for %%a in (%DEP_MAVEN%) do (
    for /f "delims=" %%u in ('dir /ad/s/b %M2_REPOSITORY%\%DEP_MAVEN%') do set DEP_MAVEN_DIR=%%u
    for %%u in (!DEP_MAVEN_DIR!\*.jar) do set GITTF_CLASSPATH=!GITTF_CLASSPATH!;%%u
)

rem  Set up debugging options
if DEFINED JAVA_DEBUG set JAVA_DEBUG_FLAGS=-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y

setlocal DISABLEDELAYEDEXPANSION

java -ea -Xmx512M %JAVA_DEBUG_FLAGS% %PROXY_PROPERTIES% -cp %GITTF_CLASSPATH% "-Dcom.microsoft.tfs.jni.native.base-directory=%BASE_DIRECTORY%\..\..\com.microsoft.gittf.core\lib\native" %LAUNCHER_CLASS% %*

set RETURN_VALUE=%errorlevel%
goto end

:end
if "%TP_NON_INTERACTIVE%" NEQ "" exit %RETURN_VALUE%
exit /B %RETURN_VALUE%
