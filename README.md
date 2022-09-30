# Cumulocity to Aris Process Mining

This microservice is a template demoing how to transfer Cumulocity data to an Aris Process Mining tenant. 

It is based on the Microservice SDK in Java and uses the Aris Data Ingestion API. 

The microservice will create **one Activity table** in Aris where the **Cases** are the Cumulocity **Device Ids** and the **Activities** are the **Measurements** for each device.
It will also generate **one Enhancement table** providing extra information on the **Cases**, aka the Cumulocity **Devices details**.  

## Pre-requisites

* One active Aris Process Mining **Enterprise** tenant
* Aris user with **Data Admin** privileges 
* Cumulocity user with **admins** and **Devicemanagement User** global roles

## Installation

1. Download the latest c8y-to-aris-ms-{version}.zip from the Releases section
2. Open the Cumulocity tenant and navigate to **Administation** application > **Applications** > **Own applications**
3. Click on "Add application" and select *Upload microservice*
4. Drag and drop the downloaded zip file into the window. Do **not** subscribe to the microservice yet; some configurations need to be done before starting the microservice.

## Preparations in Aris Process Mining
### Create a system integration for the data ingestion API

For the microservice to be able to use the Aris data ingestion API, a system integration needs to be created.

<ins>Procedure</ins>
1. Log-on to the Aris Process Mining tenant
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

The microservice needs an ARIS Process Mining data set to transfer Cumulocity data into it and also a corresponding connection to this data set for the transferred data to be stored.  

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

The microservice is hosted on the Cumulocity tenant and therefore it needs the Aris tenant details in order to connect to it. This information is configured in Cumulocity via tenant options. 

To create a tenant option, use the below Cumulocity API:
```
POST {{c8y_url}}/tenant/options
{
    "category": "aris-pm-configuration",
    "key": "apiBaseUrl",
    "value": "https://processmining.ariscloud.com"
}
```

There are six options to add before running the microservice and they shall **all** be added using the category **aris-pm-configuration**. They key and value to use are as per below:

1. **apiBaseUrl**: the value here should be the Aris tenant url (eg: https://processmining.ariscloud.com)

2. **clientId**: the value here should be the Aris Client ID provided in *step 7* of *Create a system integration for the data ingestion API*

3. **credentials.clientSecret**: the value here should be the Aris Secret Key provided in *step 7* of *Create a system integration for the data ingestion API*. Please note by adding a "credentials." prefix to the tenant option key will make the value of the option encrypted. 

4. **tenant**: the value here should be the Aris Project room name or tenant provided in *step 7* of *Create a system integration for the data ingestion API*

5. **dataset**: the value here should be the name of the data set created in *step 4* of *Create a data set and a connection to it*. **Please note that the value should be all in lower case**, for example, cumulocityiot

6. **c8yNbDaysOfMeasurements**: the value here should be an integer (eg 5) representing the date range for which the Cumulocity Measurements will be extracted. For example if the value is set to 5 then the microservice will extract all the measurements from the past 5 days. It is necessary to enter an amount of days here; indeed, in Aris Process Mining, one Case (so here a device) cannot have **more than 5000 activities**. Since the microservice is using the Measurement types as Activities, you need to make sure that the number of measurements extracted per device will not be over 5000. If it exceeeds, then the case (aka the device) and related activities (aka the measurements) will not be visible in Aris. 


## Start the Microservice and monitor it

Now that all the configurations steps are done; the microservice can start and connect to Aris. To do so and monitor the progress; perform the below steps:

1. Open the Cumulocity tenant and navigate to **Administation** application > **Applications** > **Own applications**
2. Click on the **c8y-to-aris-ms** application.
3. On the top right, click on **Subscribe**
4. Wait a few seconds and refresh the page, a **Logs** tab should now exist and after about a minute the microservice should connect successfully to Aris. It will log out all the Aris API requests made and all the responses received. It will also display any valubale information as well as warning and error messages if any. 
5. For a more user-friendly interface than the logs, the user can also visualize all the exchanges made between Cumulocity and Aris PM via the **Device Management** appplication. 
6. Navigate to the **Device Management** app > **Devices** > **All devices** > select the device **C8Y to Aris PM Agent**

7. Navigate to the **Events** tab, here you will see the progress of the exchanges between Cumulocity and Aris.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/c8y_events.PNG)

8. Note that if any warning or error is happening with the microservice, then some **Alarms** will be created too on this device. 

9. In parallele, you can connect to the Aris tenant and navigate to the data set (**Navigation menu** icon > **Data Collection** in the program header > **Data sets** > select the Cumulocity data set)
10. Navigate to **Configuration** > **Run log**
11. The log show the progress of the API call and the task should be **API uploads**.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/run_logs_processing.PNG)

12. If the connection was successful then you can see the 2 newly created source tables in **Data** > **Source tables**.
13. Also, Aris shows the current status of the data set below the data set name.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/data_set_status.PNG)

14. Once the microservice has succefully created the 2 data sets; pushed the devices and measurements data into it and commited the data to Aris, then the microservice will auto shut-down. Its work is now complete. The Run logs of Aris will be showed as successful and the data set status will be shown at **Data pending**

## Use Aris Process Mining with Cumulocity data

Now that the microservice has done its job; a user can solely use Aris PM to play with the imported Cumulocity data.

### Perform data modeling on the source tables

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
10. The **Edit relation** page opens: here click on the **DeviceId** column of each table
11. Click on **Confirm**
12. The tables are now linked to one another.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/tables_relation.PNG)

### Load the data

Now that the tables are linked, the data can be loaded and stored in the process storage.

1. Navigate to **CONFIGURATION** > **Overview** in the data set 
2. Click on **Load Data** under the **Data** column of the Overview
3. A popup will appear, click on **Run**
4. The data set status will move to **Processing Data** and it will take a few minutes until it shows **Data loaded**
5. Navigate to the **Run logs** section, and click on **Data Load**
6. Here you will see if there was no data truncated. Indeed if one device has more than 5000 measurements extracted then it will appear here that the Case relating to this device will be excluded. If the limit is not exceeded then all the process will appear in green.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/data_load.PNG)

### Analyze the data

Now that the data is in the process storage of Aris, the user can analyze it the want she/he wants to. To do so:

1. Click the **Navigation menu** icon > **Projects** in the program header.
2. Click on **New Project** and enter a name, for example CumulocityProject
3. Click on **Assign data set**
4. Select the Cumulocity data set and click on **Assign**
5. Click on **Create Analysis** and give it a name, for example CumulocityDataAnalysis
6. Click on the newly created analysis
7. Here an Aris user will know what he wants to do; aka which type of Apps he wants to add to the analysis, what type of widgets etc.. 
Below is a brief example of an analysis using the App Builder.

![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/app_builder_1.PNG)


![Image](https://github.com/SoftwareAG/cumulocity-to-aris-pm/blob/master/img/app_builder_2.PNG)