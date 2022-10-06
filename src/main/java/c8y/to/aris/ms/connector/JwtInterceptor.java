package c8y.to.aris.ms.connector;

import java.io.IOException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import okhttp3.Interceptor;
import okhttp3.Request;

public abstract class JwtInterceptor implements Interceptor {

	private final Logger logger = LoggerFactory.getLogger(getClass());

    private String jwt;
    protected String clientId;
    protected String clientSecret;
    protected String tenant;

    protected JwtInterceptor(String clientId, String clientSecret, String tenant) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tenant = tenant;
    }

    protected abstract String getToken();
    
    @Override
    public okhttp3.Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        DecodedJWT decodedJwt = null;
        if (jwt != null) {
            try {
                decodedJwt = JWT.decode(jwt);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Couldn't parse JWT", e);
            }
        }

        if (decodedJwt == null) {
            jwt = getToken();
        }

        request = request.newBuilder().header("Authorization", "Bearer " + jwt)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE).build();

        okhttp3.Response response = chain.proceed(request);

        logger.info("Response code from {} {}: {}", request.method(), request.url(), response.code());

        return response;
    }
}
