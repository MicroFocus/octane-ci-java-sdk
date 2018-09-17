# SDK for CI Plugins to integrate with ALM Octane
  
  
## Introduction

Octane's CI Plugin Development Kit provides an easy and robust way to integrate CI Environment with [ALM Octane](https://software.microfocus.com/en-us/products/alm-octane/overview) application server.
In vast majority of cases, SDK will be used as a part of a bigger integration unit - CI server's plugin/addon, therefore we'll be referring to the whole integration component as **CI Plugin** henceforth.
 
SDK knows to establish steady connectivity (by means of long polling over HTTP/S protocol) with ALM Octane, thus being able to handle tasks submitted from ALM Octane and return results if/where relevant.
There is a predefined set of tasks that are supported by such a flow (discover projects/jobs or job's/project's structure, trigger build etc).

There are 3 architectural entities that are essential to understand in order to work with SDK easily:
1) `OctaneSDK` is a top level entry point, SDK manager. This class provides static methods to add, get or remove `OctaneClient` instances.
Runtime flow will typically start from the point when `addClient` method of `OctaneSDK` is called
2) `OctaneClient` is the actual 'job doer'.
In a normal functional cases there is a need to init only one instance of `OctaneClient`, yet this class if fully thread safe and multiple instances of it may be intact.
Each `OctaneClient` is designed to provide full functionality of work with a single specific SharedSpace/Tenant of Octane server.
Client is responsible for establishing the connectivity with Octane, receive and dispatch tasks coming from Octane and provide services to push contents to Octane.
3) In order to inter-operate with the 'hosting' CI environment, SDK employs a Service Provider Interface pattern (SPI).
SPI consists of a single interface, `CIPluginServices`, that is required to be implemented by an SDK's consumer and instance of which is a must parameter of SDK's `OctaneClient` initialization method.

Typical SDK-based ALM Octane integrated CI Plugin development would walk along the following path:
1. [Including](#include-in-your-project) the SDK into the CI Plugin (be it new or an existing one) as maven dependency 
2. Implementing an SPI interface/s (or extending `CIPluginServicesBase` abstract class, convenient for partial implementation)
3. Adding [`OctaneClient` initialization](#initialization) to the initialization flow of the CI Plugin
4. Hooking into the specific CI environment's event mechanism in order:
   1. to [push builds' events](#updating-alm-octane-with-ci-events) to ALM Octane
   2. to [push builds' artifacts](#providing-test-results) (tests results etc) to ALM Octane

This Java SDK project has two sub-projects:
- **integrations-sdk**, which is the main source of the CI Plugin SDK
- **integrations-dto**, which contains the definition and building factory of all DTO objects used for communication with ALM Octane

See the [Javadoc](#creating-javadoc) for more information about the CI Plugin SDK APIs.

See [change log](changelog.md) for the released versions of this library.
  
  
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
>_This creates a javadoc site in the `/target/site/apidocs/index.html`_
  
  
## Include in Your Project

Add the following dependency to the `pom.xml` to use this SDK in your project (providing the relevant version, of course):
```
<dependency>
    <artifactId>integrations-sdk</artifactId>
    <groupId>com.hpe.adm.octane.plugins</groupId>
    <version>${integrations-sdk.version}</version>
</dependency>
```


## Usage Examples

The following CI Plugins are already using **CI Plugin SDK for ALM Octane** to connect and communicate with ALM Octane:  
[Octane Bamboo Plugin](https://github.com/MicroFocus/octane-bamboo-plugin)  
[Octane TeamCity Plugin](https://github.com/MicroFocus/octane-teamcity-plugin)


## Initialization

To start using the SDK's services, first initialize an `OctaneClient` instance. This class provides the main entry point of interaction between the SDK and its services, and interaction between the concrete CI Plugin and its services:
```java
OctaneSDK.addClient(new MyPluginServices());
```
The `addClient()` method expects to get an instance of a valid implementation of the `CIPluginServices` SPI.
This object is actually a composite API of all the endpoints to be implemented by a hosting CI Plugin for an ALM Octane use cases.
Same instance of `CIPluginServices` MAY NOT be used for more than a single `OctaneClient` initialization.
Moreover, different instances of `CIPluginServices` MAY NOT provide the same `instanceId` value, the one that is effectively identifying `OctaneClient` instances.


## Communicating with ALM Octane using Data Transfer Objects (DTO)

[Data transfer object](https://en.wikipedia.org/wiki/Data_transfer_object) (DTO) is a design pattern used to transfer data between software application subsystems. We use DTO objects to communicate data to ALM Octane.

Any DTO in the system should be created using `DTOFactory`:
```
T dto = DTOFactory.getInstance().newDTO(Class<T> targetType);
```


## Updating ALM Octane with CI Events

Most/all of the CI servers provide a means to hook into their own events system, thus providing convenient extensibility points for any addon/plugin willing to leverage the CI data further.
Upon a CI event, the CI Plugin should update ALM Octane using the `EventsService`. The steps are:

1. Subscribe to CI events by means of underlying/hosting CI system
2. Create ALM Octane's internal `CIEvent` DTO from the data provided by the CI system
3. Provide this DTO to the `publishEvent` method of `EventsService`:
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

octaneClient.getEventsService().publishEvent(ciEvent);
```

One of the capabilities of `CIEvent` is to transfer SCM data as part of it.
SCM data provided by most/all of the CI servers taking it directly from the Source Control Management tool (Git, Mercurial etc) and mostly comprises from all of the commits that was introduced into the code and built in the inspected build (aka diff from the last commit of the previous build)/
SCM data can greatly leverage integration with ALM Octane, which knows to analyze it, link it's contents to relevant Work Items (by commit patterns), provide statistical insights, failure analysis and more of it.

Pay attention, that although SCM data may be posted in the later stages of the build/pipeline run, it is always better to push the data to ALM Octane as soon as it available.
Add an SCM data to the event in the following way:
```java
ciEvent.setScmData(scmData);
```

If at the point of time, where some CI event is being pushed to ALM Octane, CI Plugin already knows that tests results are/will surely be available for this build, notifying ALM Octane about this would help to improve the performance and responsiveness of the flow.
To do so, please use the following API of `CIEvent` DTO:
```java
ciEvent.setTestResultExpected(true);
```

## ALM Octane Pipeline Structure

ALM Octane pipelines represent the flow of the CI server jobs and steps. Pipeline provides a clear, multi-level, analytic view of specific CI process, CI runs and their status so you can track product quality and progress.

ALM Octane pipeline supports a hierarchical structure of pipeline nodes. Any `PipelineNode` can contain a list of internal phases and list of post-build phases. Each `PipelinePhase` in turn contains a list of child pipeline nodes.

Pipeline nodes in internal phases represent CI nodes that complete their execution before post-build phases start to execute. Each internal phase should contain nodes that are running in parallel.

This structure is used for correct pipeline representation in ALM Octane. This way in ALM Octane, it is possible to see the flow of steps in the CI server, including which steps run in sequence and which steps run as parts of other steps.


## Providing CI Build Information

The ALM Octane server may ask for a specific build information of some pipeline. To provide the information, implement two ```CIPluginServices``` methods:

```java
SnapshotNode getSnapshotByNumber(String ciJobId, String buildCiId, boolean subTree)
```
>_This provides a snapshot of the specified build of the specified job._

```java
SnapshotNode getSnapshotLatest(String ciJobId, boolean subTree)
```
>_This provides a snapshot of the latest build of the specified job._


The main DTOs for CI build snapshots are `SnapshotNode` and `SnapshotPhase`. These provide the same hierarchical structure described in the [Pipeline Structure](#alm-octane-pipeline-structure) section and allow the ALM Octane user to see the build number, the last run date, the run status, and the duration of the run.


## Providing Test Results

We are about CI events hooking again.
Once SDK driven integration got some CI event where it can safely retrieve tests results of a build - usually that would be a finished/completed event of any build, thus we are talking on the level of a single build - it should push tests results to ALM Octane.

The preferred flow is somewhat asynchronous and it is crucial to understand it, so here is the detailed description:
1. When tests result are fully accessible, CI Plugin should ensure, that those may be identified and accessed later on; for identification we use a notion of `jobCiId` and `buildCiId` which are a simple strings; pay attention, that in some/most CI systems tests results (with their full data, like duration, exceptions etc) are not available for a prolonged period of time, in such a cases it is required to store this data somewhere for later retrieval
2. Next, CI Plugin should 'notify' the SDK that tests results are available for such a `buildCiId` of such a `jobCiId`, whatever they are, using the following API:
   ```java
   octaneClient.getTestsService().enqueuePushTestsResult(someJobCiId, someBuildCiId);
   ```
3. SDK will enqueue this info internally (not persisted, as of now) and somewhat later, usually almost immediately, but it depends on the system load of course, turn back to an SPI and will ask for a full report of tests results of the said above `jobCiId`/`buildCiId`; CI Plugin is expected to be able to retrieve the correct tests results (probably stored somewhere, as we mentioned in punkt 1 above) and return them; to be sure, relevant SPI method is:
   ```java
   TestsResult getTestsResult(String jobCiId, String buildCiId);
   ```
   >_`TestResult` DTO should contain a list of all test runs (`TestRun`) that were executed in the specified build of the specified job.
   Each `TestRun` object represents a run of a single test. It contains all information about the test, run's result (`TestRunResult` enum) and the error information if failed.
   URL to the test run report page can be provided with the `setExternalReportUrl()` method._
4. SDK will further perform the actual push to the ALM Octane


Another, less preferred way to push tests results to ALM Octane is a synchronous push, which may be done by calling the below API somewhere within punkt 1 above:
```java
octaneClient.getTestsService().pushTestsResult(pushTestsResult);
```
>_Pay attention, that usually executing time consuming actions within the CI event call effectively means holding the main CI system execution thread, since most of the CI system's events are executing on the main thread.
Don't do that._


## Disclamer update
Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017, the software is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
