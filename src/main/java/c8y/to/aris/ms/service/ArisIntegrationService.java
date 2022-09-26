package c8y.to.aris.ms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.option.TenantOptionApi;

import c8y.to.aris.ms.connector.ArisConnector;
import c8y.to.aris.ms.connector.ArisResponse;
import c8y.to.aris.ms.rest.model.SourceTable;
import c8y.to.aris.ms.rest.model.SourceTableResponse;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
	private ContextService<MicroserviceCredentials> contextService;

	@Autowired
	private ArisDatasetManager arisDatasetMgr;

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

		ArisConnector instance = new ArisConnector(properties);
		List<SourceTableResponse> sourceTables = getOrCreateArisTables(instance);
		if (sourceTables.size() > 0) {
			
		}
	}

	private List<SourceTableResponse> getOrCreateArisTables(ArisConnector arisConnector)
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


}
