@echo off
setlocal

set RUNDIR=%~dp0
set LIBPATH=%LIBPATH%;%RUNDIR%..\*;%RUNDIR%..\lib\*
rem tnt4j property override
IF ["%TNT4J_PROPERTIES%"] EQU [""] set TNT4J_PROPERTIES=%RUNDIR%..\config\tnt4j.properties
set TNT4JOPTS=-Dtnt4j.config="%TNT4J_PROPERTIES%"
rem log4j property override
IF ["%LOG4J_PROPERTIES%"] EQU [""] set LOG4J_PROPERTIES=%RUNDIR%..\config\log4j2.xml
set LOG4JOPTS=-Dlog4j2.configurationFile="%LOG4J_PROPERTIES%"
REM set LOGBACK_PROPERTIES=%RUNDIR%..\config\logback.xml
REM set LOGBACKOPTS=-Dlogback.configurationFile="%LOGBACK_PROPERTIES%"
set STREAMSOPTS=%STREAMSOPTS% %LOG4JOPTS% %TNT4JOPTS% -Dfile.encoding=UTF-8

IF ["%MAINCLASS%"] EQU [""] (
  set MAINCLASS=com.jkoolcloud.tnt4j.streams.StreamsAgent
)

set JAVA_EXEC="java"
IF ["%JAVA_HOME%"] EQU [""] (
  echo "JAVA_HOME" env. variable is not defined!..
) else (
  echo Will use java from: "%JAVA_HOME%"
  set JAVA_EXEC="%JAVA_HOME%\bin\java"
)

@echo on
%JAVA_EXEC% %STREAMSOPTS% -classpath "%LIBPATH%" %MAINCLASS% %*
