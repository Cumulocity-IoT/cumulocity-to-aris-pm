package c8y.to.aris.ms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.event.CumulocitySeverities;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.alarm.AlarmApi;
import com.cumulocity.sdk.client.event.EventApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.InventoryFilter;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import com.cumulocity.sdk.client.measurement.MeasurementFilter;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import com.cumulocity.sdk.client.measurement.MeasurementCollection;

import c8y.IsDevice;
import c8y.to.aris.ms.App;
import c8y.to.aris.ms.connector.ArisConnector;
import c8y.to.aris.ms.connector.ArisResponse;
import c8y.to.aris.ms.integration.ArisDatasetManager;
import c8y.to.aris.ms.rest.model.CycleState;
import c8y.to.aris.ms.rest.model.DataUploadResponse;
import c8y.to.aris.ms.rest.model.IngestionCycleRequest;
import c8y.to.aris.ms.rest.model.IngestionCycleResponse;
import c8y.to.aris.ms.rest.model.ReadyForIngestionRequest;
import c8y.to.aris.ms.rest.model.ReadyForIngestionResponse;
import c8y.to.aris.ms.rest.model.SourceTable;
import c8y.to.aris.ms.rest.model.SourceTableResponse;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ArisIntegrationService<I extends ArisConnector> {
	public static final String TENANT_OPTIONS_ARIS_CATEGORY = "aris-pm-configuration";

	public static final String TENANT_OPTIONS_KEY_ARIS_URL = "apiBaseUrl";
	public static final String TENANT_OPTIONS_KEY_CLIENT_ID = "clientId";
	public static final String TENANT_OPTIONS_KEY_CLIENT_SECRET = "clientSecret";
	public static final String TENANT_OPTIONS_KEY_TENANT_ID = "tenant";
	public static final String TENANT_OPTIONS_KEY_DATASET = "dataset";
	public static final String TENANT_OPTIONS_KEY_C8Y_MEASUREMENT_DAYS = "c8yNbDaysOfMeasurements";

	@Autowired
	private MicroserviceSubscriptionsService subscriptionsService;

	@Autowired
	private TenantOptionApi tenantOptionApi;

	@Autowired
	private MeasurementApi measurementApi;

	@Autowired
	private InventoryApi inventoryApi;
	
	@Autowired
	private AlarmApi alarmApi;
	
	@Autowired
	private EventApi eventApi;

	@Autowired
	private ArisDatasetManager arisDatasetMgr;

	private ArisConnector arisConnector;
	private Properties properties;
	private ManagedObjectRepresentation microserviceAgent;

	protected final Logger logger = LoggerFactory.getLogger(ArisIntegrationService.class);

	@EventListener
	private void init(MicroserviceSubscriptionAddedEvent event) {
		logger.info("Connecting to Aris Process Mining from tenant {}", subscriptionsService.getTenant());
		microserviceAgent = getAgent();
		
		List<OptionRepresentation> optionRepresentations = tenantOptionApi.getAllOptionsForCategory(TENANT_OPTIONS_ARIS_CATEGORY);
		properties = new Properties();
		if (optionRepresentations.size() != 6) {
			logger.error("The tenant options have not been set for this Microservice. A total of 6 tenant options need to exist:" + System.lineSeparator() +
					"aris-pm-configuration.apiBaseUrl" + System.lineSeparator() +
					"aris-pm-configuration.clientId" + System.lineSeparator() +
					"aris-pm-configuration.credentials.clientSecret" + System.lineSeparator() +
					"aris-pm-configuration.tenant" + System.lineSeparator() +
					"aris-pm-configuration.dataset" + System.lineSeparator() +
					"aris-pm-configuration.c8yNbDaysOfMeasurements");
			logger.error("Please create them and restart the microservice");
			generateAlarmForMicroservice("The tenant options have not been set for the Aris Microservice. A total of 6 tenant options should exist.", 
					"ms_configuration", CumulocitySeverities.CRITICAL);
			App.shutdownMicroservice();
			return;
		}
		
		for (OptionRepresentation optionRepresentation : optionRepresentations) {
			properties.setProperty(optionRepresentation.getKey(), optionRepresentation.getValue());
		}

		arisConnector = new ArisConnector(properties);
		List<SourceTableResponse> sourceTables = getOrCreateArisTables();
		if (sourceTables.size() > 0) {
			startDataUpload();
		}

	}
	
	private ManagedObjectRepresentation getAgent()
	{
		Iterator<ManagedObjectRepresentation> device = inventoryApi.getManagedObjectsByFilter(new InventoryFilter().byFragmentType("ms_ArisAgent"))
				.get(1).iterator();
		
		if (device.hasNext()) {
			return device.next();
		} else
		{
			//create MO
			ManagedObjectRepresentation mor = new ManagedObjectRepresentation();
			mor.setName("C8Y to Aris PM Agent");
			mor.setType("ms_agent");
			mor.setProperty("ms_ArisAgent", new HashMap<Object, Object>());
			mor.set(new IsDevice());
			mor = inventoryApi.create(mor);
			return mor;
		}
	}

	private List<SourceTableResponse> getOrCreateArisTables()
	{
		logger.info("Creating Source Tables in Aris...");

		ArisResponse<List<SourceTableResponse>> arisResponse =  arisConnector.getSourceTable();
		List<SourceTableResponse> sourceTables = new ArrayList<SourceTableResponse>();

		boolean activityTableExist = false;
		boolean enhancementTableExist = false;
		if (arisResponse.isOk()) {
			List<SourceTableResponse> existingSourceTables = arisResponse.getResult();
			for (SourceTableResponse table : existingSourceTables)
			{
				if (table.getName().compareTo(ArisDatasetManager.ACTIVITY_TABLE_NAME) == 0) {
					sourceTables.add(table);
					activityTableExist = true;
				} else if (table.getName().compareTo(ArisDatasetManager.ENHANCEMENT_TABLE_NAME) == 0) {
					sourceTables.add(table);
					enhancementTableExist = true;
				}
			}
			if (sourceTables.size() == 2) {
				return sourceTables;
			}
			else {
				List<SourceTable> arisSourceTables = new ArrayList<SourceTable>();

				if (!activityTableExist) {
					logger.info("Trying to create Activity table....");
					SourceTable activityTable = this.arisDatasetMgr.createActivitySourceTableStructure();
					arisSourceTables.add(activityTable);
				}
				if (!enhancementTableExist) {
					logger.info("Trying to create Enhancement table....");
					SourceTable enhancementTable = this.arisDatasetMgr.createEnhancementSourceTableStructure();
					arisSourceTables.add(enhancementTable);
				}
				arisResponse = arisConnector.createSourceTables(arisSourceTables);
				if (arisResponse.isOk()) {
					logger.info("Source Tables successfully created on Aris");
					generateEventForMicroservice("The source tables have been successfully created on Aris.", "ms_arisApiResponse");
					return arisResponse.getResult();
				} else {
					logger.error("Error while creating Source Tables on Aris: " + arisResponse.getMessage());
					generateAlarmForMicroservice("Error while creating Source Tables on Aris using the /sourceTables API : " + System.lineSeparator() + 
							arisResponse.getMessage(), "ms_api_sourceTables", CumulocitySeverities.CRITICAL);
				}
			}
		}
		else {
			logger.error("Error while retrieving Source Tables on Aris: " + arisResponse.getMessage());
			generateAlarmForMicroservice("Error while retrieving Source Tables on Aris using the /sourceTableDefinitions API : " + System.lineSeparator() + 
					arisResponse.getMessage(), "ms_api_sourceTableDefinitions", CumulocitySeverities.CRITICAL);
		}
		return sourceTables;

	}

	private void startDataUpload()
	{
		logger.info("Checking if the dataset is ready for data upload");
		//1. Check if the dataset is ready for data upload
		ReadyForIngestionRequest readyForIngestionRequest = this.arisDatasetMgr.getIngestionRequest();
		ArisResponse<ReadyForIngestionResponse> isReadyForIngestion = arisConnector.isDatasetReadyForDataUpload(readyForIngestionRequest);
		if (isReadyForIngestion.isOk()) {
			ReadyForIngestionResponse response = isReadyForIngestion.getResult();
			if (response.isReady()) {
				logger.info("The dataset is ready for data upload!");
				generateEventForMicroservice("The Aris dataset is ready for data upload.", "ms_arisApiResponse");
				//2. Create a data upload cycle
				createDataUploadCycle();
			} else
			{
				//wait for 30 seconds and try again.
				try {
					logger.warn("The dataset is not ready yet for ingestion: Cause provided from ARIS " + response.getCause().getCode() + " : " + response.getCause().getMessage());
					logger.warn("Pausing for 30sec and trying again...");
					generateAlarmForMicroservice("The Aris dataset is not ready yet for ingestion. Message from the /readyForIngestion API : " + System.lineSeparator() + 
							response.getCause().getCode() + " - " + response.getCause().getMessage(), "ms_api_readyForIngestion", CumulocitySeverities.WARNING);
					generateEventForMicroservice("The Aris dataset is not ready yet for ingestion, re-trying in 30 seconds...", "ms_arisApiResponse");
					Thread.sleep(30000);
					startDataUpload();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}
		}
		else {
			logger.error("Error while checking if the dataset is ready for ingestion: " + isReadyForIngestion.getMessage());
			generateAlarmForMicroservice("Error while checking if the Aris dataset is ready for ingestion using the /readyForIngestion API : " + System.lineSeparator() + 
					isReadyForIngestion.getMessage(), "ms_api_readyForIngestion", CumulocitySeverities.CRITICAL);
		}
	}

	private void createDataUploadCycle()
	{
		logger.info("Creating a Data Upload Cycle...");
		IngestionCycleRequest ingestionCycle = this.arisDatasetMgr.getIngestionCycleRequest();
		ArisResponse<IngestionCycleResponse> cycle = arisConnector.createIngestionCycle(ingestionCycle);
		if (cycle.isOk()) {
			IngestionCycleResponse response = cycle.getResult();
			if (response.getState().getValue().compareTo("ACCEPTING_DATA") == 0) {
				logger.info("The data upload cycle state is ACCEPTING_DATA!");
				generateEventForMicroservice("The Aris data upload cycle state is ACCEPTING_DATA.", "ms_arisApiResponse");
				//3. Upload the data
				uploadC8YDataToAris(response.getKey());
			} else
			{
				//wait for 30 seconds and try again.
				try {
					logger.warn("The dataset is not accepting data at the moment: Current Cycle state " + response.getState().getValue());
					logger.warn("Potential resolution: log on to ARIS, navigate to \"Run log\" within your dataset and cancel any pending task.");
					logger.warn("Pausing for 30sec and trying again...");
					generateAlarmForMicroservice("The Aris dataset is not accepting data at the moment: Current Cycle state : " + System.lineSeparator() + 
							response.getState().getValue(), "ms_api_ingestionCycles", CumulocitySeverities.WARNING);
					generateEventForMicroservice("The Aris dataset is accepting data at the moment, re-trying in 30 seconds...", "ms_arisApiResponse");
					
					Thread.sleep(30000);
					createDataUploadCycle();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}
		}
		else {
			logger.error("Error while creating the data upload cycle : " + cycle.getMessage());
			generateAlarmForMicroservice("Error while creating the Aris data upload cycle using the /ingestionCycles API : " + System.lineSeparator() + 
					cycle.getMessage(), "ms_api_ingestionCycles", CumulocitySeverities.CRITICAL);
		}
	}

	private void uploadC8YDataToAris(String ingestionCycleKey)
	{
		logger.info("Start uploading data in the Activity Table " + this.arisDatasetMgr.getFullyQualifiedNameActivityTable() + "...");
		//Start with ActivityTable
		//Warning: In Aris PM, one case cannot have more thank 5K activities. Since the microservice is using the measurement types as activities of a case (aka a device)
		// you need to make sure the period for which you extract the measurement wont exceed the 5K measurements for any of the devices. 
		//If it exceeds, then you wont be able to see the case nor the activities in Aris
		List<List<Object>> activityTableData = retrieveMeasurementsForNDays();
		ArisResponse<DataUploadResponse> uploadResponse = arisConnector.uploadDataToSoureTable(this.arisDatasetMgr.getFullyQualifiedNameActivityTable(), activityTableData);

		if (uploadResponse.isOk()) {
			DataUploadResponse response = uploadResponse.getResult();
			if (response.isSuccessful()) {
				logger.info("The upload to the Activity Table " + this.arisDatasetMgr.getFullyQualifiedNameActivityTable() + " was successful!");
				logger.info("Start uploading data in the Enhancement Table " + this.arisDatasetMgr.getFullyQualifiedNameEnhancementTable() + "...");
				generateEventForMicroservice("The upload to the Activity Table " + this.arisDatasetMgr.getFullyQualifiedNameActivityTable() + " was successful.", "ms_arisApiResponse");
				
				//continue with Enhancement table
				List<List<Object>> enhancementTableData = retrieveDevicesData();
				uploadResponse = arisConnector.uploadDataToSoureTable(this.arisDatasetMgr.getFullyQualifiedNameEnhancementTable(), enhancementTableData);

				if (uploadResponse.isOk()) {
					response = uploadResponse.getResult();
					if (response.isSuccessful()) {
						logger.info("The upload to the Enhancement Table " + this.arisDatasetMgr.getFullyQualifiedNameEnhancementTable() + " was successful!");	
						generateEventForMicroservice("The upload to the Enhancement Table " + this.arisDatasetMgr.getFullyQualifiedNameEnhancementTable() + " was successful.", "ms_arisApiResponse");
						
						//4. Commit the data
						commitC8YDataToAris(ingestionCycleKey);
					}
					else
					{
						//wait for 30 seconds and try again.
						try {
							logger.warn("The data upload for the Enhancement table was not successful.");
							logger.warn("Potential resolution: log on to ARIS, navigate to \"Run log\" within your dataset and cancel any pending task.");
							logger.warn("Pausing for 30sec and trying again...");
							generateAlarmForMicroservice("The Aris data upload for the Enhancement table was not successful.", "ms_api_sourceTables_ET_data", CumulocitySeverities.WARNING);
							generateEventForMicroservice("The Aris data upload for the Enhancement table was not successful, re-trying in 30 seconds...", "ms_arisApiResponse");
							
							Thread.sleep(30000);
							uploadC8YDataToAris(ingestionCycleKey);
						} catch (InterruptedException e) {
							e.printStackTrace();
						} 
					}
				}
				else {
					logger.error("Error while uploading data to the table : " + uploadResponse.getMessage());
					generateAlarmForMicroservice("Error while uploading data to the Enhancement table using the /sourceTables/{sourceTable}/data API : " + System.lineSeparator() + 
							uploadResponse.getMessage(), "ms_api_sourceTables_ET_data", CumulocitySeverities.CRITICAL);
				}
			} else
			{
				//wait for 30 seconds and try again.
				try {
					logger.warn("The data upload for the Activity table was not successful.");
					logger.warn("Potential resolution: log on to ARIS, navigate to \"Run log\" within your dataset and cancel any pending task.");
					logger.warn("Pausing for 30sec and trying again...");
					generateAlarmForMicroservice("The Aris data upload for the Activity table was not successful.", "ms_api_sourceTables_AT_data", CumulocitySeverities.WARNING);
					generateEventForMicroservice("The Aris data upload for the Activity table was not successful, re-trying in 30 seconds...", "ms_arisApiResponse");
					
					
					Thread.sleep(30000);
					uploadC8YDataToAris(ingestionCycleKey);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}
		}
		else {
			logger.error("Error while uploading data to the table : " + uploadResponse.getMessage());
			generateAlarmForMicroservice("Error while uploading data to the Activity table using the /sourceTables/{sourceTable}/data API : " + System.lineSeparator() + 
					uploadResponse.getMessage(), "ms_api_sourceTables_AT_data", CumulocitySeverities.CRITICAL);
		}
	}

	private List<List<Object>> retrieveMeasurementsForNDays()
	{
		List<List<Object>> activityData = new ArrayList<List<Object>>();
		Map<String, Integer> nbMeasurementsBySource = new HashMap<String, Integer>();
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DAY_OF_MONTH, - Integer.parseInt(properties.get(TENANT_OPTIONS_KEY_C8Y_MEASUREMENT_DAYS).toString()));
		Date fromDate = cal.getTime();
		MeasurementCollection measurements = measurementApi.getMeasurementsByFilter(new MeasurementFilter().byFromDate(fromDate));
		Iterator<MeasurementRepresentation> pastMeasurements = measurements.get(2000).allPages().iterator();

		while (pastMeasurements.hasNext()) {
			MeasurementRepresentation mr = pastMeasurements.next();
			String deviceId = mr.getSource().getId().getValue();
			if (!nbMeasurementsBySource.containsKey(deviceId)) {
				nbMeasurementsBySource.put(deviceId, 0);
			}
			int nbMeasurements = nbMeasurementsBySource.get(deviceId);
			
			List<List<Object>> measurementSeriesAsCsv = this.arisDatasetMgr.buildActivityData(mr);
			Stream.of(measurementSeriesAsCsv).forEach(activityData::addAll);
			nbMeasurementsBySource.put(deviceId, nbMeasurements + measurementSeriesAsCsv.size());
		}

		for (String d : nbMeasurementsBySource.keySet()) {
			if (nbMeasurementsBySource.get(d) > 5000) {
				generateAlarmForMicroservice("There is " + nbMeasurementsBySource.get(d) + " measurements retrieved for device id " + d + System.lineSeparator() + 
						". In Aris PM, one case cannot have more thank 5K activities. Since the microservice is using the measurement types as activities, " + System.lineSeparator() + 
						" you will not be able to see the case nor the activities in Aris. Please reduce the period in the Tenant Option c8yNbDaysOfMeasurements to extract less measurements." ,
						"ms_nbMeasurementsExceeded5K_MO_"+d, CumulocitySeverities.MAJOR);
			}
		}
		return activityData;
	}

	private List<List<Object>> retrieveDevicesData()
	{
		List<List<Object>> enhancementData = new ArrayList<List<Object>>();

		Iterator<ManagedObjectRepresentation> devices = inventoryApi.getManagedObjectsByFilter(new InventoryFilter().byFragmentType("c8y_IsDevice"))
				.get(2000).allPages().iterator();

		while (devices.hasNext()) {
			ManagedObjectRepresentation mo = devices.next();
			
			//we dont want to extract the MO created for this microservice
			if (mo.hasProperty("ms_ArisAgent")) {
				continue;
			}
			List<Object> devicesInfoAsCsv = this.arisDatasetMgr.buildEnhancementData(mo);
			enhancementData.add(devicesInfoAsCsv);
		}

		return enhancementData;
	}

	private void commitC8YDataToAris(String ingestionCycleKey)
	{
		logger.info("Start commiting the uploaded data...");

		ArisResponse<IngestionCycleResponse> cycle = arisConnector.commitDataToSourceTable(ingestionCycleKey);
		if (cycle.isOk()) {
			IngestionCycleResponse response = cycle.getResult();
			manageCycleStates(ingestionCycleKey, response.getState());
		}
		else {
			logger.error("Error while commiting the data upload cycle : " + cycle.getMessage());
			generateAlarmForMicroservice("Error while commiting the data upload cycle to Aris using the ingestionCycles/{injectionCycleKey}/dataComplete API : " + System.lineSeparator() + 
					cycle.getMessage(), "ms_api_ingestionCycles_dataComplete", CumulocitySeverities.CRITICAL);
		}
	}

	private void retrieveCycleState(String ingestionCycleKey)
	{
		logger.info("Retrieving the cycle state...");
		ArisResponse<CycleState> cycle = arisConnector.getCycleState(ingestionCycleKey);
		if (cycle.isOk()) {
			CycleState response = cycle.getResult();
			manageCycleStates(ingestionCycleKey, response);
		}
		else {
			logger.error("Error while retrieving the data upload cycle : " + cycle.getMessage());
			generateAlarmForMicroservice("Error while retrieving the data upload cycle from Aris using the ingestionCycles/{injectionCycleKey}/state API : " + System.lineSeparator() + 
					cycle.getMessage(), "ms_api_ingestionCycles_state", CumulocitySeverities.CRITICAL);
		}
	}

	private void manageCycleStates(String ingestionCycleKey, CycleState cycle) //, boolean isDataUpload)
	{
		if (cycle.getValue().compareTo("COMPLETED_SUCCESSFULLY") == 0) {
//			if (isDataUpload)
//			{
				logger.info("All data was commited successfully in ARIS data sets! ");
				generateEventForMicroservice("All data was commited successfully in ARIS data sets! The Microservice will shut-down.", "ms_arisApiResponse");
				App.shutdownMicroservice();
				
				//5. Check if data set ready to load the data in the process storage
//				startDataLoadInProcessStorage();
//			} else
//			{
//				//this is data load to process storage
//				logger.info("All data was loaded successfully in ARIS! ");
//				logger.info("You can now start visualize Cumulocity processes in your dataset.");
//				logger.info("This Microservice will now auto-shutdown..");
//				App.shutdownMicroservice();
//			}
		} else if (cycle.getValue().compareTo("INGESTING_DATA") == 0) {
			logger.info("The data is still being ingested.. Waiting for 10 seconds before checking the cycle state again");
			generateEventForMicroservice("The Cumulocity data is still being ingested in ARIS... Waiting for 10 seconds before checking the cycle state again", "ms_arisApiResponse");
			try {
				Thread.sleep(10000);
				retrieveCycleState(ingestionCycleKey); //, isDataUpload);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
		} else if (cycle.getValue().compareTo("CANCELED") == 0) {
			logger.info("The cycle state has been cancelled...");
			generateEventForMicroservice("The Aris cycle state has been cancelled... Waiting for 30 seconds before trying to data upload again.", "ms_arisApiResponse");
			
//			if (isDataUpload)
//			{
				logger.info("Trying the whole data upload steps again in 30 seconds...");
				try {
					Thread.sleep(30000);
					startDataUpload();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
//			}
//			else
//			{
//				//this is data load to process storage
//				logger.info("Trying the whole data load to process storage again in 30 seconds...");
//				try {
//					Thread.sleep(30000);
//					startDataLoadInProcessStorage();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				} 
//			}
		}
		else //FAILED
		{
			logger.warn("There was an issue while commiting the data: Current Cycle state " + cycle.getValue());
//			String log = isDataUpload == true ? " data upload " : " data load in process storage ";
			logger.warn("This cycle will be cancelled and the data upload tried again.");
			generateAlarmForMicroservice("There was an issue while commiting the data using the ingestionCycles/{injectionCycleKey}/dataComplete API " + System.lineSeparator() + 
					": Current Cycle state " + cycle.getValue(), "ms_api_ingestionCycles_ick_dataComplete", CumulocitySeverities.CRITICAL);
			generateEventForMicroservice("There was an issue while commiting the data... This cycle will be cancelled and the data upload tried again.", "ms_arisApiResponse");
			
			cancelCycle(ingestionCycleKey); //, isDataUpload);
		}
	}

	private void cancelCycle(String ingestionCycleKey)
	{
		logger.info("Cancelling the cycle state...");
		ArisResponse<CycleState> cycle = arisConnector.cancelCycle(ingestionCycleKey);
		if (cycle.isOk()) {
			logger.info("The cycle state has been cancelled...");
//			String log = isDataUpload == true ? " data upload " : " data load in process storage ";
			logger.info("Trying the whole data upload steps again in 30 seconds...");
			generateEventForMicroservice("The Aris cycle state has been cancelled... Waiting for 30 seconds before trying to data upload again.", "ms_arisApiResponse");
			
			try {
				Thread.sleep(30000); 
//				if (isDataUpload) {
					startDataUpload();
//				} else {
//					startDataLoadInProcessStorage();
//				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
		}
		else {
			logger.error("Error while cancelling the data upload cycle : " + cycle.getMessage());
			generateAlarmForMicroservice("Error while cancelling the data upload cycle in Aris using the ingestionCycles/{injectionCycleKey}/canceled API : " + System.lineSeparator() + 
					cycle.getMessage(), "ms_api_ingestionCycles_canceled", CumulocitySeverities.CRITICAL);
		}
	}
	
	private void generateAlarmForMicroservice(String alarmText, String alarmType, CumulocitySeverities severity)
	{
		AlarmRepresentation a = new AlarmRepresentation();
		a.setSource(microserviceAgent);
		a.setDateTime(new DateTime());
		a.setText(alarmText);
		a.setType(alarmType);
		a.setSeverity(severity.name());
		
		alarmApi.create(a);
	}
	
	private void generateEventForMicroservice(String eventText, String eventType)
	{
		EventRepresentation e = new EventRepresentation();
		e.setSource(microserviceAgent);
		e.setDateTime(new DateTime());
		e.setText(eventText);
		e.setType(eventType);
		
		eventApi.create(e);
	}

//	private void startDataLoadInProcessStorage() {
//		logger.info("Checking if dataset is ready to load the data in the process storage...");
//		ReadyForIngestionRequest readyForIngestionRequest = this.arisDatasetMgr.getIngestionRequest();
//		ArisResponse<ReadyForIngestionResponse> isReadyForIngestion = arisConnector.isDatasetReadyForDataUpload(readyForIngestionRequest);
//		if (isReadyForIngestion.isOk()) {
//			ReadyForIngestionResponse response = isReadyForIngestion.getResult();
//			if (response.isReady()) {
//				logger.info("The dataset is ready to load the data into the process storage");
//				//6. Start the data load
//				loadDataInProcessStorage();
//			} else
//			{
//				try {
//					logger.warn("The dataset is not ready yet to load the data into the process storage: Cause provided from ARIS " + response.getCause().getCode() + " : " + response.getCause().getMessage());
//					logger.warn("Pausing for 30sec and trying again...");
//					Thread.sleep(30000);
//					startDataLoadInProcessStorage();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				} 
//			}
//		}
//		else {
//			logger.error("Error while checking if the dataset is ready for ingestion: " + isReadyForIngestion.getMessage());
//		}
//	}
//
//	private void loadDataInProcessStorage()
//	{
//		logger.info("Starting the data load...");
//		IngestionCycleRequest ingestionCycle = this.arisDatasetMgr.getIngestionCycleRequest();
//		ArisResponse<IngestionCycleResponse> cycle = arisConnector.createIngestionCycle(ingestionCycle);
//		if (cycle.isOk()) {
//			IngestionCycleResponse response = cycle.getResult();
//			manageCycleStates(response.getKey(), response.getState(), false);
//		}
//		else {
//			logger.error("Error while creating the data upload cycle : " + cycle.getMessage());
//		}
//	}
}

