package c8y.to.aris.ms.connector;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


import c8y.to.aris.ms.controller.ArisAuthService;
import c8y.to.aris.ms.controller.ArisRESTController;
import c8y.to.aris.ms.rest.model.SourceTable;
import c8y.to.aris.ms.rest.model.SourceTableColumn;
import c8y.to.aris.ms.rest.model.Token;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
public class ArisConnector {
	private ArisAuthService arisAuthService;
	private ArisRESTController arisRestController;
	private Properties properties = new Properties();


	class DXAdminJWTInterceptor extends JwtInterceptor {


		public DXAdminJWTInterceptor(String clientId, String clientSecret, String tenant) {
			super(clientId, clientSecret, tenant);
		}


		@Override
		protected String getToken() {
			String token = null;
			try {
				Response<Token> response;
				response = arisAuthService.getToken("client_credentials", this.clientId, this.clientSecret, this.tenant)
						.execute();
				if (response.isSuccessful() && response.body() != null) {
					token = response.body().getApplicationToken();
					log.info("Successfully received a JWT: {}", token);
				} else {
					log.error("Can't obtain a JWT with the following reponse: {}", response.errorBody().string());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return token;
		}


	}

	public ArisConnector(Properties properties) {
		this.setProperties(properties);
	}


	public void setProperties(Properties properties) {
		this.properties = properties;
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Couldn't start connector.", e);
		}
	}

	protected void init() {
		HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
		interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
		OkHttpClient arisClient = new OkHttpClient.Builder().addInterceptor(
				new DXAdminJWTInterceptor(properties.getProperty("clientId"), properties.getProperty("clientSecret"), properties.getProperty("tenant")))
				.addInterceptor(interceptor)
				.build();

		//TODO : Properties should be loaded from tenant option. In tenant option we want url, username, pwd and tenant, and dataset.
		//url should be  https://processmining.ariscloud.com
		String url = properties.getProperty("aris.pm.apiBaseUrl");

		OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

		Retrofit core = new Retrofit.Builder().client(arisClient)
				.baseUrl(url + "/mining/api/pub/dataIngestion/v1/dataSets/")
				.addConverterFactory(JacksonConverterFactory.create()).build();

		Retrofit auth = new Retrofit.Builder().baseUrl(url + "/umc/api/oauth/")
				.client(client).addConverterFactory(JacksonConverterFactory.create()).build();
		arisRestController = core.create(ArisRESTController.class);
		arisAuthService = auth.create(ArisAuthService.class);
		try
		{
		    SourceTableColumn stb = new SourceTableColumn();
		    stb.setDatatype("STRING");
		    stb.setName("column_1");
		    
		    SourceTable st = new SourceTable();
		    List<SourceTableColumn> columns = new ArrayList<SourceTableColumn>();
		    columns.add(stb);
		    st.setColumns(columns);
		    st.setName("tableMel");
		    st.setNamespace("default");
		    
		    List<SourceTable> tables = new ArrayList<SourceTable>();
		    tables.add(st);
			Response<String> response = arisRestController.createSourceTables("Training",tables).execute();
			if (response.isSuccessful()) {

			}else {
				log.error("Error while retrieving the list of gateways: {}", response.errorBody().string());

			}
		} catch (Exception e) {
			e.printStackTrace();

		}
	}
}
