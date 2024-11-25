# Getting Started
Welcome to the log reader! This little app will return files in reversed order

## Running the service
Once you've cloned the repo, you can run it right away with default settings:

```
./gradlew clean build bootRun
```

You can then pop over to http://localhost:8080/logs to take a look at the logs in the directory!

Take a look at the query options to see what else you can do 

If you'd like to use something other than the default settings, take a look at the 
configuration options to see what environment variables you can set to modify it's behavior.

## Query Parameters
A couple of query parameters are available on the logs endpoint, see below for brief descriptions 
of them.

### fileName
You may specify a directory or file within the configured directory to return

**Example:** http://localhost:8080/logs?fileName=numberFile.txt

### logLines
You may specify a maximum number of lines to return for any given logfile. This will be applied to
all files returned

**Example:** http://localhost:8080/logs?logLines=10

### searchTerm
You may specify a search term which will act as a case-sensitive filter for log lines - only lines
containing that search term will be returned

**Example:** http://localhost:8080/logs?searchTerm=test

## Configuration Options
There's a few config options available to you when starting the application and can all 
be set as environment variables.

### LOG_DIRECTORY
This specifies the direction to scrape for logs. Users will be disallowed from requesting 
any files outside this directory.

This directory must be an absolute path.

**Default:** /var/logs

### DEFAULT_LOG_LINE_LIMIT
This specifies the maximum number of lines to return if none is provided in the API.

This can be set to a negative number to disable this check and allow the API to request all lines from a log.

**Default:** 10000


### LOG_SERVER_NAME
A user-friendly name for the server if desired

**Default:** none