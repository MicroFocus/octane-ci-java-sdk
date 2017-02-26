# CI Plugin SDK for ALM Octane

<br>
## Introduction

A Java SDK that should be used by CI plugin to connect and communicate with ALM Octane. See the [Javadoc](#creating-javadoc) for more information of CI Plugin SDK API.

This project has two sub-projects:

- **integrations-dto** which contains definition and building factory of all DTO objects used in communication with ALM Octane.

- **integrations-sdk** which is the main source of the CI Plugin SDK

<br>
The easiest way to compile the project is to use [maven](https://maven.apache.org/) and run the command:
```
mvn clean install
```
from the root directory.

<br>
## Creating JavaDoc

In order to create javadoc run the following [maven](https://maven.apache.org/) command from the project root directory:
```
mvn javadoc:aggregate
```
This will create a javadoc site in the ```/target/site/apidocs/index.html```

<br>
## Initialization

To start using CI Plugin SDK, we need to initialize OctaneSDK instance. This class provides main entry point of interaction between SDK and it&#39;s services, and interaction between concrete CI plugin and it&#39;s services.
```java
OctaneSDK.init(new MyPluginServices(), true);
```
The ```init()``` method expects an object that implements ```CIPluginServices``` interface. This object is actually a composite API of all the endpoints to be implemented by a hosting CI Plugin for ALM Octane use cases.

<br>
## DTO

[Data transfer object](https://en.wikipedia.org/wiki/Data_transfer_object) (DTO), is a design pattern used to transfer data between software application subsystems. We use DTO objects to communicate data to ALM Octane.

Any DTO in the system should be created using ```DTOFactory```.
```
T dto = DTOFactory.getInstance().newDTO(Class<T> targetType);
```

<br>
## CI events

Upon CI event, CI plugin needs to update ALM Octane via ```EventsService``` object. The steps are:

1. Get CI event.
2. Create CIEvent DTO using the info from CI event and CI environment.
3. Provide this DTO to the publishEvent method of EventsService.

```java
CIEvent ciEvent = dtoFactoryInstance.newDTO(CIEvent.class)
      .setEventType(eventType)
      .setCauses(causes)
      .setProject(project)
      .setProjectDisplayName(displayName)
      .setBuildCiId(buildCiId)
      .setEstimatedDuration(estimatedDuration)
      .setStartTime(startTime)
      .setPhaseType(phaseType);

OctaneSDK.getInstance().getEventsService().publishEvent(ciEvent);
```

<br>
## SCM data

SCM data - data provided by Source Control Management tool about changes in source code reflected in a specific CI build. SCM data should be submitted to ALM Octane as part of the ```CIEvent``` DTO.
```
ciEvent.setScmData(scmData);
```

<br>
## Pipeline structure

ALM Octane pipelines represent the flow of the CI server jobs and steps. Pipeline provides a clear, multi-level, analytic view of specific CI process, CI runs and their status to track and monitor product quality and progress.

ALM Octane pipeline supports hierarchical structure of pipeline nodes. Any ```PipelineNode``` can contain list of internal phases and list of post build phases. Each ```PipelinePhase``` in turn contains a list of child pipeline nodes.

Pipeline nodes in internal phases represent CI nodes that complete their execution before post build phases start to execute. Each internal phase should contain nodes that are running in parallel.

This structure is used for correct pipeline representation in ALM Octane. This way in ALM Octane, it is possible to see the flow of steps in the CI server, including which steps run in sequence and which steps run as parts of other steps.

<br>
## CI build

ALM Octane server may ask for a specific build information of some pipeline. Two ```CIPluginServices``` methods need to be implemented for this matter:

```java
SnapshotNode getSnapshotByNumber(String ciJobId, String buildCiId, boolean subTree)
```
>_Provides Snapshot of the specified CI Build of the specified CI Job._

<br>
```java
SnapshotNode getSnapshotLatest(String ciJobId, boolean subTree)
```
>_Provides Snapshot of the latest CI Build of the specified CI Job_

<br>
The main DTOs for CI build snapshots are ```SnapshotNode``` and ```SnapshotPhase```, that provide the same hierarchical structure described in the Pipeline structure section. This way ALM Octane user can also see the build number, the last run date, the run status, and the duration of the run.

<br>
## Test results

Test results for specific build are provided by ```CIPluginServices.getTestsResult``` method.

```java
TestsResult getTestsResult(String jobId, String buildNumber)
```
>_Provides tests result report for the specific build. ```TestResult``` DTO should contain a list of all test runs (```TestRun```) that were executed in the job cpecified by the ```jobId```._

<br>
Each ```TestRun``` object represents a single test that ran in a specific CI build. It contains all the information of this specific test, run result status (```TestRunResult``` enum) and error information in case the test failed. Also a url to the test report page can be provided via ```setExternalReportUrl()``` method.
