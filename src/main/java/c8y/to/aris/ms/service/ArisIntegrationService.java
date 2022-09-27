package c8y.to.aris.ms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.InventoryFilter;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import com.cumulocity.sdk.client.measurement.MeasurementFilter;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import com.cumulocity.sdk.client.measurement.MeasurementCollection;

import c8y.to.aris.ms.connector.ArisConnector;
import c8y.to.aris.ms.connector.ArisResponse;
import c8y.to.aris.ms.integration.ArisDatasetManager;
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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import java.util.stream.Stream;
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



	//	public static final String PROPERTIES_ACTILITY_JWT_TOKEN = "actility.jwt";

	@Autowired
	private MicroserviceSubscriptionsService subscriptionsService;

	@Autowired
	private TenantOptionApi tenantOptionApi;
	
	@Autowired
	private MeasurementApi measurementApi;
	
	@Autowired
	private InventoryApi inventoryApi;

	@Autowired
	private ArisDatasetManager arisDatasetMgr;

	private ArisConnector arisConnector;
	
	protected final Logger logger = LoggerFactory.getLogger(ArisIntegrationService.class);

	@EventListener
	private void init(MicroserviceSubscriptionAddedEvent event) {
		logger.info("Connecting to Aris Process Mining from tenant {}", subscriptionsService.getTenant());

		List<OptionRepresentation> optionRepresentations = tenantOptionApi.getAllOptionsForCategory(TENANT_OPTIONS_ARIS_CATEGORY);
		Properties properties = new Properties();
		for (OptionRepresentation optionRepresentation : optionRepresentations) {
			properties.setProperty(optionRepresentation.getKey(), optionRepresentation.getValue());
		}
		
		//TODO generate an agent MO for the MS
		//		mor = getGateway(gatewayProvisioning.getGwEUI().toLowerCase());
		//		if (mor == null) {
		//			mor = createGateway(lnsConnectorId, lnsResponse.getResult());
		//		}

		arisConnector = new ArisConnector(properties);
		List<SourceTableResponse> sourceTables = getOrCreateArisTables();
		if (sourceTables.size() > 0) {
			startDataUpload();
		}

	}

	private List<SourceTableResponse> getOrCreateArisTables()
	{
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
					//TODO generate an event on the agent
					//			EventRepresentation event = new EventRepresentation();
					//			event.setType("Gateway provisioned");
					//			event.setText("Gateway has been provisioned");
					//			event.setDateTime(new DateTime());
					//			event.setSource(mor);
					//			eventApi.create(event);
					return arisResponse.getResult();
				} else {
					logger.error("Error while creating Source Tables on Aris: " + arisResponse.getMessage());
					//TODO create alarm
				}
			}
		}
		else {
			logger.error("Error while retrieving Source Tables on Aris: " + arisResponse.getMessage());
			//TODO create alarm
		}
		return sourceTables;


	
	
	}
	
	private void startDataUpload()
	{
		//1. Check if the dataset is ready for data upload
		ReadyForIngestionRequest readyForIngestionRequest = this.arisDatasetMgr.getIngestionRequest();
		ArisResponse<ReadyForIngestionResponse> isReadyForIngestion = arisConnector.isDatasetReadyForDataUpload(readyForIngestionRequest);
		if (isReadyForIngestion.isOk()) {
			ReadyForIngestionResponse response = isReadyForIngestion.getResult();
			if (response.isReady()) {
				//2. Create a data upload cycle
				createDataUploadCycle();
			} else
			{
				//wait for 10 seconds and try again.
				try {
					logger.warn("The dataset is not ready yet for ingestion: Cause provided from ARIS " + response.getCause().toString());
					logger.warn("Pausing for 30sec and trying again...");
					Thread.sleep(30000);
					startDataUpload();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}
		}
		else {
			logger.error("Error while checking if the dataset is ready for ingestion: " + isReadyForIngestion.getMessage());
		}
	}
	
	private void createDataUploadCycle()
	{
		IngestionCycleRequest ingestionCycle = this.arisDatasetMgr.getIngestionCycleRequest();
		ArisResponse<IngestionCycleResponse> cycle = arisConnector.createIngestionCycle(ingestionCycle);
		if (cycle.isOk()) {
			IngestionCycleResponse response = cycle.getResult();
			if (response.getState().getValue().compareTo("ACCEPTING_DATA") == 0) {
				//3. Upload the data
				uploadC8YDataToAris(response.getKey());
			} else
			{
				//wait for 10 seconds and try again.
				try {
					logger.warn("The dataset is not accepting data at the moment: Current Cycle state " + response.getState().getValue());
					logger.warn("Potential resolution: log on to ARIS, navigate to \"Run log\" within your dataset and cancel any pending task.");
					logger.warn("Pausing for 30sec and trying again...");
					Thread.sleep(30000);
					createDataUploadCycle();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}
		}
		else {
			logger.error("Error while creating the data upload cycle : " + cycle.getMessage());
		}
	}
	
	private void uploadC8YDataToAris(String ingestionCycleKey)
	{
		//Start with ActivityTable
		List<List<Object>> activityTableData = retrieveMeasurementsForPastWeek();
		ArisResponse<DataUploadResponse> uploadResponse = arisConnector.uploadDataToSoureTable(this.arisDatasetMgr.getFullyQualifiedNameActivityTable(), activityTableData);
	
		if (uploadResponse.isOk()) {
			DataUploadResponse response = uploadResponse.getResult();
			if (response.isSuccessful()) {
				//continue with Enhancement table
				List<List<Object>> enhancementTableData = retrieveDevicesData();
			} else
			{
				//wait for 30 seconds and try again.
				try {
					logger.warn("The data upload for the Activity table was not successful.");
					logger.warn("Potential resolution: log on to ARIS, navigate to \"Run log\" within your dataset and cancel any pending task.");
					logger.warn("Pausing for 30sec and trying again...");
					Thread.sleep(30000);
					uploadC8YDataToAris(ingestionCycleKey);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}
		}
		else {
			logger.error("Error while uploading data to the table : " + uploadResponse.getMessage());
		}
	}
	
	private List<List<Object>> retrieveMeasurementsForPastWeek()
	{
		List<List<Object>> activityData = new ArrayList<List<Object>>();
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.WEEK_OF_YEAR, -1);
		Date fromDate = cal.getTime();
		MeasurementCollection measurements = measurementApi.getMeasurementsByFilter(new MeasurementFilter().byFromDate(fromDate));
		Iterator<MeasurementRepresentation> pastMonthMeasurements = measurements.get(2000).allPages().iterator();
		
		while (pastMonthMeasurements.hasNext()) {
			MeasurementRepresentation mr = pastMonthMeasurements.next();
			List<List<Object>> measurementSeriesAsCsv = this.arisDatasetMgr.buildActivityData(mr);
			Stream.of(measurementSeriesAsCsv).forEach(activityData::addAll);
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
			List<List<Object>> devicesInfoAsCsv = this.arisDatasetMgr.buildEnhancementData(mo);
			Stream.of(devicesInfoAsCsv).forEach(enhancementData::addAll);
		}
		
		return enhancementData;
	}


}
