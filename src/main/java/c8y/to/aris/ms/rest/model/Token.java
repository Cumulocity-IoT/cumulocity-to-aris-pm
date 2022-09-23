package c8y.to.aris.ms.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Token {
    @JsonProperty("applicationToken")
    private String applicationToken;

	public String getApplicationToken() {
		return applicationToken;
	}

	public void setApplicationToken(String applicationToken) {
		this.applicationToken = applicationToken;
	}

  
   
}
