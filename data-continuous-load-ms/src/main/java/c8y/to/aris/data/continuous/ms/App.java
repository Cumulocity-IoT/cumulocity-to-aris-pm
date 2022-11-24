package c8y.to.aris.data.continuous.ms;

import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;

@MicroserviceApplication
public class App {
	protected static final Logger logger = LoggerFactory.getLogger(App.class);
	
    public static void main (String[] args) {
    	SpringApplication.run(App.class, args);
    }
 
}