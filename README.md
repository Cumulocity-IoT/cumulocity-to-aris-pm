# Cumulocity to ARIS Process Mining

This project is a template demoing how to transfer Cumulocity data to an ARIS Process Mining tenant. It is composed of two microservices: 

* the first microservice creates the source tables in ARIS and upload the first set of data within it.

* the second one loads the data in the process storage of ARIS. This can be done only once an ARIS' user modeled the source tables and created any potential needed transformation. After loading the data, the microservice polls at regular interval the data from Cumulocity to send it to ARIS Process Mining in a continuous stream.

Both microservices are based on the Microservice SDK in Java and uses the ARIS Data Ingestion API. 

The first microservice, called *"c8y-to-aris-add-tbl-ms"*, will create **one Activity table** in ARIS where the **Cases** are the Cumulocity **Device Ids** and the **Activities** are the **Measurements** for each device.
It will also generate **one Enhancement table** providing extra information on the **Cases**, aka the Cumulocity **Devices details**.  

## Pre-requisites

* One active ARIS Process Mining **Enterprise** tenant
* ARIS user with **Data Admin** privileges 
* Cumulocity user with **admins** and **Devicemanagement User** global roles

## Installation

1. Download the latest **c8y-to-aris-add-tbl-ms-{version}.zip** from the Releases section
2. Open the Cumulocity tenant and navigate to **Administation** application > **Applications** > **Own applications**
3. Click on "Add application" and select *Upload microservice*
4. Drag and drop the downloaded zip file into the window. Do **not** subscribe to the microservice yet; some configurations need to be done before starting the microservice.
5. Perform again steps 1 to 4 with the latest **c8y-to-aris-dataload-ms-{version}.zip** from the Releases section

## Preparations in ARIS Process Mining
### Create a system integration for the data ingestion API

For the microservices to be able to use the ARIS data ingestion API, a system integration needs to be created.

<ins>Procedure</ins>
1. Log-on to the ARIS Process Mining tenant
2. Click the **Navigation menu** icon > **Administration** in the program header.
3. Click **System integration** in the **Administration** panel.
4. Click **Add system integration > Data ingestion (API)**. The corresponding dialog opens.
5. Enter a name, for example, Cumulocity Data ingestion, and an optional description.
6. Select the authentication method **Client credentials** in the **Grant type (OAuth)** drop-down menu.
7. Click **Add**. The **Data ingestion access data** dialog opens. The dialog provides the client ID, secret key, and project room name.
8. Save the authentication data, for example, using a text editor. **All of the 3 piece of data will be needed when configuring Cumulocity later on.** 
9. Click **Done**.
The system integration is created and listed with the name you specified.

### Create a data set and a connection to it

The microservices needs an ARIS Process Mining data set to transfer Cumulocity data into it and also a corresponding connection to this data set for the transferred data to be stored.  

<ins>Procedure</ins>
1. Click the **Navigation menu** icon > **Data Collection** in the program header.
2. Click **Data sets** in the **Data Collection** panel.
3. Click **New data set**. The corresponding dialog opens.
4. Enter a name, for example, CumulocityIoT, and click on **Create**.
5. The newly created data set automaticall opens. 
6. Open the **Integration > Connections** component.
7. Click **Add connection**. If you add a connection to a source system for the first time and you have not assigned a 'Living Process' license to the data set yet, the **Assign ‘Living Process’ license** dialog opens.
8. Select a license in the drop-down menu. You need the ‘Living Process’ license to extract and analyze processes. The number of processes you can extract depends on the
selected license.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/aris_living_process_license.PNG)

9. Click **Assign**. The **Add connection** dialog opens.
10. Configure the connection.
    
    a. Enter a unique name for the connection to the source system, for example, Cumulocity.
    
    b. Select the system integration created for the data ingestion API (in the previous section).
    
    c. Click **Add**.

You have created a connection for the API. The created connection is displayed on the **Connections** page with the settings you specified.

## Preparations in Cumulocity

The microservices are hosted on the Cumulocity tenant and therefore they need the ARIS tenant details in order to connect to it. This information is configured in Cumulocity via tenant options. 

To create a tenant option, use the below Cumulocity API:
```
POST {{c8y_url}}/tenant/options
{
    "category": "aris-pm-configuration",
    "key": "apiBaseUrl",
    "value": "https://processmining.ariscloud.com"
}
```

There are seven options to add before running the microservices and they shall **all** be added using the category **aris-pm-configuration**. They key and value to use are as per below:

1. **apiBaseUrl**: the value here should be the ARIS tenant url (eg: https://processmining.ariscloud.com)

2. **clientId**: the value here should be the ARIS Client ID provided in *step 7* of *Create a system integration for the data ingestion API*

3. **credentials.clientSecret**: the value here should be the ARIS Secret Key provided in *step 7* of *Create a system integration for the data ingestion API*. Please note by adding a "credentials." prefix to the tenant option key will make the value of the option encrypted. 

4. **tenant**: the value here should be the ARIS Project room name or tenant provided in *step 7* of *Create a system integration for the data ingestion API*

5. **dataset**: the value here should be the name of the data set created in *step 4* of *Create a data set and a connection to it*. **Please note that the value should be all in lower case**, for example, cumulocityiot

6. **c8yNbDaysOfMeasurements**: the value here should be an integer (eg 5) representing the date range for which the Cumulocity Measurements will be extracted the first time they are extracted. For example if the value is set to 5 then the first microservice will extract all the measurements from the past 5 days. It is necessary to enter an amount of days here; indeed, in ARIS Process Mining, one Case (so here a device) cannot have **more than 5000 activities**. Since the microservice is using the Measurement types as Activities, you need to make sure that the number of measurements extracted per device will not be over 5000. If it exceeeds, then the case (aka the device) and related activities (aka the measurements) will not be visible in ARIS. Please note this is used only the first time it will extract the measurements; thereafter the measurements will be polled on a rolling window basis by the second microservice.

7. **pollingIntervalInMinutes**: the value here indicates the polling interval in minutes to retrieve the Cumulocity Measurements and Managed objects. On start-up, the first microservice will retrieve the Measurements for the past **c8yNbDaysOfMeasurements** days; afterwards, the second microservice will retrieve them every X minutes, using the latest polling time as the **fromDate** parameter of the Cumulocity REST API. This will ensure the microservice retrieves the devices and measurements on a rolling window.


## Start the Microservice and monitor it

Now that all the configurations steps are done; the first microservice can start and connect to ARIS. To do so and monitor the progress; perform the below steps:

1. Open the Cumulocity tenant and navigate to **Administation** application > **Applications** > **Own applications**
2. Click on the **c8y-to-aris-add-tbl-ms** application.
3. On the top right, click on **Subscribe**
4. Wait a few seconds and refresh the page, a **Logs** tab should now exist and after about a minute the microservice should connect successfully to ARIS. It will log out all the ARIS API requests made and all the responses received. It will also display any valubale information as well as warning and error messages if any. 
5. For a more user-friendly interface than the logs, the user can also visualize all the exchanges made between Cumulocity and ARIS PM via the **Device Management** appplication. 
6. Navigate to the **Device Management** app > **Devices** > **All devices** > select the device **C8Y to ARIS PM Agent**

7. Navigate to the **Events** tab, here you will see the progress of the exchanges between Cumulocity and ARIS.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/c8y_events.PNG)

8. Note that if any warning or error is happening with the microservice, then some **Alarms** will be created too on this device. 

9. In parallele, you can connect to the ARIS tenant and navigate to the data set (**Navigation menu** icon > **Data Collection** in the program header > **Data sets** > select the Cumulocity data set)
10. Navigate to **Configuration** > **Run log**
11. The log show the progress of the API call and the task should be **API uploads**.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/run_logs_processing.PNG)

12. If the connection was successful then you can see the 2 newly created source tables in **Data** > **Source tables**.
13. Also, ARIS shows the current status of the data set below the data set name.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/data_set_status.PNG)

14. Once the microservice has succefully created the 2 data sets; pushed the devices and measurements data into it and commited the data to ARIS, then the microservice job is done here. It is time now for the user to configure the source table in the **Data Modeling** section of ARIS Process Mining. The Run logs of ARIS will be showed as successful and the data set status will be shown at **Data pending**

### Perform data modeling on the source tables

When the microservice has committed successfully the Cumulocity data to the source tables, it will generate an event. The Event displayed in Cumulocity will have the text: 
*All data was commited successfully in ARIS data sets! You can now connect to the ARIS Process Mining tenant to configure the tables.*

Connect to ARIS Process Mining and perform the below steps:

1. Navigate to **DATA** > **Data modeling** in the data set 
2. Click on **Add tables** and select the 2 newly created source tables
3.  Select the **cumulocityMeasurements_ActivityTable** and click on **change table role**
4. Select the type **Activity table** > Next 
5. Select
    * **DeviceId** column as the **Case ID** 
    * **MeasurementType** column as the **Activity**
    * **Time** as the **Start Time**
6. Click on **OK**

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/activity_table_setup.PNG)

7. You should now see the 2 sources tables in the data modeling page with **cumulocityMeasurements_ActivityTable** set as an **Activity table**
8. Click on **Add Relation** on the table **cumulocityMeasurements_ActivityTable** 
9. Click on **Connect with table** on the table **cumulocityDeviceDetails_EnhancementTable**
10. The **Edit relation** page opens: here click on the **DeviceId** column of each table. Ensure that the relation is 1 Activity for N Devices otherwise it would not work. You can click on the "Swap tables" button to ensure you have the same relation than as per below image.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/relation-creation.PNG)

11. Click on **Add relation**
12. The tables are now linked to one another.
13. Click on **Configure merge key** on the Enhancement table.
14. Select **DeviceId** and click on **Set**


![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/tables_relation.PNG)

### Data Load in the Process Storage

Now that the tables are linked, the data can be loaded and stored in the process storage. It is now time to subscribe to the second microservice which will automatically try to load the data. It will first check if the data set is ready for the data load. If the response from ARIS is that it is ready the the data load will kick off and the user can follow the progress via the Cumulocity Events or via the Run Logs of ARIS. However, if ARIS responds that the data set is not ready for the data load (for example the user is creating some transformations); then the microservice will wait and try again every 2 minutes. A Cumulocity Alarm will be generated as well to alert the user that the data load attempt did not succeed.
To start the second microservice, follow the below steps:

1. Open the Cumulocity tenant and navigate to **Administation** application > **Applications** > **Own applications**
2. Click on the **c8y-to-aris-dataload-ms** application.
3. On the top right, click on **Subscribe**

When the data load is triggered successfully by the microservice, the user can visualize the progress as well on ARIS side. 

1. The data set status moves to **Processing Data** and it will take a few minutes until it shows **Data loaded**
2. Navigate to the **Run logs** section, and click on **Data Load**
3. Here you will see if there was no data truncated. Indeed if one device has more than 5000 measurements extracted then it will appear here that the Case relating to this device will be excluded. If the limit is not exceeded then all the process will appear in green.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/data_load.PNG)

## Use ARIS Process Mining with Cumulocity data

Now that the microservice has done its job; a user can solely use ARIS PM to play with the imported Cumulocity data.

### Analyze the data

Now that the data is in the process storage of ARIS, the user can analyze it the want she/he wants to. To do so:

1. Click the **Navigation menu** icon > **Projects** in the program header.
2. Click on **New Project** and enter a name, for example CumulocityProject
3. Click on **Assign data set**
4. Select the Cumulocity data set and click on **Assign**
5. Click on **Create Analysis** and give it a name, for example CumulocityDataAnalysis
6. Click on the newly created analysis
7. Here an ARIS user will know what he wants to do; aka which type of Apps he wants to add to the analysis, what type of widgets etc.. 
Below is a brief example of an analysis using the App Builder.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/app_builder_1.PNG)


![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/app_builder_2.PNG)


## Adapt the Microservice to your needs

Those microservices are a template which can be re-used as is or adapted to meet customer needs. For example you might want to extract a different Activity table or maybe extract more columns from Cumulocity.

There are only 2 Java classes which need to be modified if you want to retrieve different data from Cumulocity:

1. c8y.to.aris.ms.integration.**ArisDatasetManager** :  this is the class which defines the Activity and Enhancement table. It sets the name, the structure of the table aka the columns and type for each one and it populates the table by returning each Cumulocity data as a comma delimited line (structure required by ARIS API).

2. c8y.to.aris.ms.service.**ArisIntegrationService** : this is the class doing most of the job of the microservice. It retrieves the tenant options, calls the necessary classes to connect to ARIS and then it follows the main cycle of the ARIS Ingestion API to build the source tables and commit the data to them. It also calls the Cumulocity API by extracting the Measurements and Devices data; for each row returned it will then call the methods of the **ArisDatasetManager** class so it can populate the source tables correctly and entirely.

So if there is a need to change the number of ARIS source tables, the structure of them and the data within it, then you will need to modify the two aboves classes and then re-compile the microservice to reflect the changes.


--------------

These tools are provided as-is and without warranty or support. They do not constitute part of the Software AG product suite. Users are free to use, fork and modify them, subject to the license agreement. While Software AG welcomes contributions, we cannot guarantee to include every contribution in the master project.