package com.tactical.backend.tactical_host;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.tactical.backend.commons.GlobalVariables;
import com.tactical.backend.commons.utils.ResourceUtils;
import com.tactical.backend.model.AwsEnvironment;
import com.tactical.backend.tactical_host.model.ConfigYaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayKitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayKitService.class.getSimpleName());

    private final ObjectMapper mapper;

    private ConfigYaml configYaml;

    public GatewayKitService() {
        this.mapper = new ObjectMapper(new YAMLFactory());

        // Ignore unknown fields (future-proofing)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Optional: accept missing fields gracefully
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
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
        //false = running so we return it inverted
        return !GlobalVariables.getStopRequested();
    }

    public void loadGatewayYamlFile(String filename) throws JsonProcessingException {
        String yamlContent = ResourceUtils.getInstance().load(filename);

        configYaml = mapper.readValue(yamlContent, ConfigYaml.class);
    }
}