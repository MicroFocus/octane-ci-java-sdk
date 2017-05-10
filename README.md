# CI Plugin SDK for ALM Octane
  
  
## Introduction

A Java SDK that should be used by the CI plugin to connect and communicate with ALM Octane. See the [Javadoc](#creating-javadoc) for more information about the CI Plugin SDK API.

This Java SDK project has two sub-projects:

- **integrations-dto**, which contains the definition and building factory of all DTO objects used for communication with ALM Octane.

- **integrations-sdk**, which is the main source of the CI Plugin SDK.
  
  
## Compiling the Project

The easiest way to compile the project is to use [Maven](https://maven.apache.org/) and run the following command from the root directory:
```
mvn clean install
```
  
  
## Creating JavaDoc

To create Javadoc, run the following [Maven](https://maven.apache.org/) command from the project root directory:
```
mvn javadoc:aggregate
```
>_This creates a javadoc site in the ```/target/site/apidocs/index.html```_
  
  
## Include in Your Project

Add the following dependency to the pom.xml to use this SDK in your project:
```
<dependency>
    <artifactId>integrations-sdk</artifactId>
    <groupId>com.hpe.adm.octane.plugins</groupId>
    <version>1.0</version>
</dependency>
```


## Usage Examples

The following CI plugins already use **CI Plugin SDK for ALM Octane** to connect and communicate with ALM Octane:  
[Octane Bamboo Plugin](https://github.com/HPSoftware/octane-bamboo-plugin)  
[Octane TeamCity Plugin](https://github.com/HPSoftware/octane-teamcity-plugin)


## Initializing

To start using the CI Plugin SDK, first initialize an OctaneSDK instance. This class provides the main entry point of interaction between the SDK and its services, and interaction between the concrete CI plugin and its services.
```java
OctaneSDK.init(new MyPluginServices(), true);
```
The ```init()``` method expects an object that implements the ```CIPluginServices``` interface. This object is actually a composite API of all the endpoints to be implemented by a hosting CI Plugin for ALM Octane use cases.


## Communicating with ALM Octane using Data Transfer Objects (DTO)

[Data transfer object](https://en.wikipedia.org/wiki/Data_transfer_object) (DTO) is a design pattern used to transfer data between software application subsystems. We use DTO objects to communicate data to ALM Octane.

Any DTO in the system should be created using ```DTOFactory```:
```
T dto = DTOFactory.getInstance().newDTO(Class<T> targetType);
```


## Updating ALM Octane with CI Events

Upon a CI event, the CI plugin must update ALM Octane using the ```EventsService``` object. The steps are:

1. Get the CI event.
2. Create the ```CIEvent``` DTO using the info from the CI event and the CI environment.
3. Provide this DTO to the ```publishEvent``` method of ```EventsService```.

```java
CIEvent ciEvent = DTOFactory.getInstance().newDTO(CIEvent.class)
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


## Submitting Source Control Management (SCM) Data

SCM data are provided by the Source Control Management tool about changes in source code reflected in a specific CI build. SCM data should be submitted to ALM Octane as part of the ```CIEvent``` DTO.
```
ciEvent.setScmData(scmData);
```


## ALM Octane Pipeline Structure

ALM Octane pipelines represent the flow of the CI server jobs and steps. Pipeline provides a clear, multi-level, analytic view of specific CI process, CI runs and their status so you can track product quality and progress.

ALM Octane pipeline supports a hierarchical structure of pipeline nodes. Any ```PipelineNode``` can contain a list of internal phases and list of post-build phases. Each ```PipelinePhase``` in turn contains a list of child pipeline nodes.

Pipeline nodes in internal phases represent CI nodes that complete their execution before post-build phases start to execute. Each internal phase should contain nodes that are running in parallel.

This structure is used for correct pipeline representation in ALM Octane. This way in ALM Octane, it is possible to see the flow of steps in the CI server, including which steps run in sequence and which steps run as parts of other steps.


## Providing CI Build Information

The ALM Octane server may ask for a specific build information of some pipeline. To provide the information, implement two ```CIPluginServices``` methods:

```java
SnapshotNode getSnapshotByNumber(String ciJobId, String buildCiId, boolean subTree)
```
>_This provides a snapshot of the specified CI build of the specified CI job._

```java
SnapshotNode getSnapshotLatest(String ciJobId, boolean subTree)
```
>_This provides a snapshot of the latest CI build of the specified CI job._


The main DTOs for CI build snapshots are ```SnapshotNode``` and ```SnapshotPhase```. These provide the same hierarchical structure described in the [Pipeline Structure](#alm-octane-pipeline-structure) section and allow the ALM Octane user to see the build number, the last run date, the run status, and the duration of the run.


## Providing Test Results

Test results for a specific build are provided by ```CIPluginServices.getTestsResult``` method.

```java
TestsResult getTestsResult(String jobId, String buildNumber)
```
>_This provides test result report for the specific build. The ```TestResult``` DTO should contain a list of all test runs (```TestRun```) that were executed in the job specified by the ```jobId```._


Each ```TestRun``` object represents a single test that ran in a specific CI build. It contains all the information of this specific test, the run result status (```TestRunResult``` enum) and error information if the test failed. Also a url to the test report page can be provided with the ```setExternalReportUrl()``` method.
