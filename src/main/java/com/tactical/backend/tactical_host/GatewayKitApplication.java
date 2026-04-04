package com.tactical.backend.tactical_host;

import com.tactical.backend.commons.BuildVersion;
import com.tactical.backend.commons.PerfCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
//import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
//@EnableSwagger2
public class GatewayKitApplication extends SpringBootServletInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayKitApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GatewayKitApplication.class, args);
        int servicePort = context.getBean(Environment.class).getProperty("server.port", Integer.class, 8080);
        LOGGER.info("GatewayKit is using port " + servicePort);
        PerfCounter.get("..Build_Version", BuildVersion.getBuildVersion()).increment();
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(GatewayKitApplication.class);
    }

}
