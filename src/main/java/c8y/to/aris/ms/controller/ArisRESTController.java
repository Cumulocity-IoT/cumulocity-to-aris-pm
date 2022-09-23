package c8y.to.aris.ms.controller;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


import c8y.to.aris.ms.rest.model.SourceTable;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;


@RestController
public interface ArisRESTController {
	
	final Logger logger = LoggerFactory.getLogger(ArisRESTController.class);

	@Headers({"Content-Type: application/json", "Accept: application/json"})
	@POST("{datasetRef}/sourceTables?forceReplace=false")
	Call<String> createSourceTables(@Path("datasetRef") String datasetRef, @Body List<SourceTable> sourceTables);

}
