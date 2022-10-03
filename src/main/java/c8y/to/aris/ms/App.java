package c8y.to.aris.ms;

import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;

@MicroserviceApplication
public class App {
	protected static final Logger logger = LoggerFactory.getLogger(App.class);
//	static ConfigurableApplicationContext appCtx;
	
    public static void main (String[] args) {
    	SpringApplication.run(App.class, args);
    }
    
//    public static void shutdownMicroservice()
//	{
//    	
//		SpringApplication.exit(appCtx, new ExitCodeGenerator() {
//			
//			@Override
//			public int getExitCode() {
//				// TODO Auto-generated method stub
//				return 0;
//			}
//		});
//	}
}