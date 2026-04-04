package com.tactical.backend.tactical_host;

import com.tactical.backend.commons.GlobalVariables;
import com.tactical.backend.model.AwsEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayKitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayKitService.class.getSimpleName());

    public GatewayKitService() {
    }

    public void start() {
        LOGGER.info("Starting with Environment " + AwsEnvironment.getCurrent());
        GlobalVariables.setStopRequested(false);
    }

    public void stop() {
        if (GlobalVariables.getStopRequested()) {
            LOGGER.info("Already stopped");
            return; // already stopped
        }

        GlobalVariables.setStopRequested(true);
    }

    public boolean getControllerStatus() {
        //false = running so we return it inversed
        return !GlobalVariables.getStopRequested();
    }

}