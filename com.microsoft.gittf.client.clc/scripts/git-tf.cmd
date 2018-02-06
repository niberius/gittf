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
rem  Script for launching git-tf.  Configures the classpath and starts Java
rem  with the git-tf main class.
rem

setlocal
setlocal ENABLEEXTENSIONS
setlocal ENABLEDELAYEDEXPANSION

set BASE_DIRECTORY=%~dp0

if not exist "%BASE_DIRECTORY%lib\com.microsoft.gittf.client.clc-*.jar" goto missingJar

set GITTF_CLASSPATH=

for %%i in ("%BASE_DIRECTORY%lib\*.jar") do set GITTF_CLASSPATH=!GITTF_CLASSPATH!;"%%i"

setlocal DISABLEDELAYEDEXPANSION

java -ea -Xmx512M -cp %GITTF_CLASSPATH% "-Dcom.microsoft.tfs.jni.native.base-directory=%BASE_DIRECTORY%native" com.microsoft.gittf.client.clc.Main %*

set RETURN_VALUE=%errorlevel%
goto end

:missingJar
echo Unable to find a required JAR: %BASE_DIRECTORY%\lib\com.microsoft.gittf.client.clc.jar does not exist
set RETURN_VALUE=1

:end
if "%GITTF_NON_INTERACTIVE%" NEQ "" exit %RETURN_VALUE%
exit /B %RETURN_VALUE%
