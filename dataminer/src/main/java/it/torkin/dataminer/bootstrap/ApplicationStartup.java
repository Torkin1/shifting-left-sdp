package it.torkin.dataminer.bootstrap;

import java.util.logging.Logger;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = Logger.getLogger(ApplicationStartup.class.getName());

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        
        logger.info("Application started");  

    }
    
}
