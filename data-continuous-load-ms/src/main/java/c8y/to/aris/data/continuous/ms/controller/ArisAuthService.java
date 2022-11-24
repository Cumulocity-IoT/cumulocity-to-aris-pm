package c8y.to.aris.data.continuous.ms.controller;


import c8y.to.aris.data.continuous.ms.rest.model.Token;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ArisAuthService {
	@FormUrlEncoded
	@POST("apptoken")
	Call<Token> getToken(@Field("grant_type") String grantType, @Field("client_id") String clientId, @Field("client_secret") String clientSecret, @Field("tenant") String tenant);
}
