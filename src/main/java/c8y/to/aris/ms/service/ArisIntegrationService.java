package c8y.to.aris.ms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.sdk.client.option.TenantOptionApi;

import c8y.to.aris.ms.connector.ArisConnector;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ArisIntegrationService<I extends ArisConnector> {
	public static final String TENANT_OPTIONS_PROVIDER_TOKEN_KEY = "provider.token";

	public static final String TENANT_OPTIONS_ACTILITY_CATEGORY = "actility";

	public static final String PROPERTIES_ACTILITY_JWT_TOKEN = "actility.jwt";

	@Autowired
	private MicroserviceSubscriptionsService subscriptionsService;

	@Autowired
	private TenantOptionApi tenantOptionApi;

	@Autowired
	private ContextService<MicroserviceCredentials> contextService;

	protected final Logger logger = LoggerFactory.getLogger(ArisIntegrationService.class);

	@EventListener
	private void init(MicroserviceSubscriptionAddedEvent event) {
		logger.info("Connecting to Aris Process Mining from tenant {}", subscriptionsService.getTenant());
		
//		List<OptionRepresentation> optionRepresentations = tenantOptionApi.getAllOptionsForCategory(ais.getType());
//		Properties properties = new Properties();
//		for (OptionRepresentation optionRepresentation : optionRepresentations) {
//			properties.setProperty(optionRepresentation.getKey(), optionRepresentation.getValue());
//		}
//		properties.setProperty(PROPERTIES_ACTILITY_JWT_TOKEN, readActilityJWTTokenFromC8YTenantOptions());
//		properties.setProperty("id",mor.getId().getValue());
//		properties.setProperty("name",mor.getName());
//		properties.setProperty("type",ais.getType());
		Properties properties = new Properties();
		properties.setProperty("aris.pm.apiBaseUrl","https://processmining.ariscloud.com");
		properties.setProperty("clientId","a41c6df6-9df3-4ccc-aeee-7eb4cd855aed");
		properties.setProperty("clientSecret","7e60776c-5fce-4374-8ccc-82afc796fe71");
		properties.setProperty("tenant","trainingmel1");
		ArisConnector instance = new ArisConnector(properties);
		//add the connector in memory

	}

	//		private String readActilityJWTTokenFromC8YTenantOptions() {
	//			OptionPK optionPK = new OptionPK();
	//			optionPK.setKey(TENANT_OPTIONS_PROVIDER_TOKEN_KEY);
	//			optionPK.setCategory(TENANT_OPTIONS_ACTILITY_CATEGORY);
	//			OptionRepresentation optionRepresentation = tenantOptionApi.getOption(optionPK);
	//			if (optionRepresentation.getValue() != null) {
	//				String encodedToken = optionRepresentation.getValue();
	//				try {
	//					return encryptionService.decryptString(encodedToken, ACTILITY_KEY);
	//				} catch (DecryptFailedException e) {
	//					logger.error("Can't decrypt Actility JWT token: {}", e.getMessage());
	//				}
	//			}
	//			return "";
	//		}

}
