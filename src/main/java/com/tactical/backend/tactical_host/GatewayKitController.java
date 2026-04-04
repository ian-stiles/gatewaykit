package com.tactical.backend.tactical_host;

import com.tactical.backend.commons.Delay;
import com.tactical.backend.commons.PerfCounter;
import com.tactical.backend.commons.utils.EndpointUtils;
import com.tactical.backend.commons.utils.ResourceUtils;
import com.tactical.backend.gwt_model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
public class GatewayKitController {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayKitController.class);

    private GatewayKitService gatewayKitService;

    private static final Object lockGatewayKitServiceCreation = new Object();

    private boolean firstTimeHealthCheck = true;

    private String controlHtml;

    private final long serviceStartTime = System.currentTimeMillis();

    public GatewayKitController() {
        EndpointUtils.setApplicationName("GatewayKit");

        try {
            controlHtml = ResourceUtils.getInstance().load("control.html");
        } catch (Exception ex) {
            LOGGER.error("Cannot load control.html file from resource directory.", ex);
        }
    }

    private GatewayKitService getGatewayKitService(boolean autoStart) {
        if (gatewayKitService == null) {
            synchronized (lockGatewayKitServiceCreation) {
                if (gatewayKitService == null) {
                    gatewayKitService = new GatewayKitService();
                    String gatewayFilePath = System.getProperty("GATEWAY_FILE");
                    gatewayKitService.loadGatewayYamlFile(gatewayFilePath);

                    if (autoStart) {
                        gatewayKitService.start();
                    }
                }
            }
        }
        return gatewayKitService;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Standard functions
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @RequestMapping(value = "/control.html", method = RequestMethod.GET)
    @ApiIgnore
    public String control(@RequestParam Map<String, String> parms, HttpServletResponse httpServletResponse) {
        String actionUrl = parms.get("actionUrl");
        if (!StringUtils.isNullOrEmpty(actionUrl)) {
            switch (actionUrl) {
                case "start":
                    start();
                    break;
                case "stop":
                    stop();
                    break;
                case "healthcheck":
                    return getHealthStatus();
                case "reset-counters":
                    resetCounters();
                    break;
                case "terminate":
                    terminate(null);
                    break;
            }

        }
        return controlHtml;
    }

    @RequestMapping(value = "/start", method = RequestMethod.GET)
    @ApiIgnore
    public void start() {
        LOGGER.info("Endpoint: start");
        getGatewayKitService(true).start();
    }

    @RequestMapping(value = "/stop", method = RequestMethod.GET)
    @ApiIgnore
    public void stop() {
        LOGGER.info("Endpoint: stop");

        boolean[] recognizedStop = new boolean[1];

        Thread stoppingThread = new Thread("stoppingThread") {
            @Override
            public void run() {
                getGatewayKitService(false).stop();
                recognizedStop[0] = true;
            }
        };
        stoppingThread.start();

        final int waitTimeMs = 5 * 60 * 1000;
        long endTimeMs = System.currentTimeMillis() + waitTimeMs;
        while (!recognizedStop[0] && System.currentTimeMillis() < endTimeMs) {
            Delay.millisecs(100);
        }

        if (recognizedStop[0]) {
            LOGGER.info("Stopped when idle");
        } else {
            LOGGER.info("Stop request timed-out");
        }
    }

    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public String getHealthStatus() {

        long uptimeMs = System.currentTimeMillis() - serviceStartTime;

        final String healthy = "{\"status\":healthy,\"uptime_seconds\":" + uptimeMs / 1000 + "}";
        final String unhealthy = "{\"status\":unhealthy}";
        try {
            if (firstTimeHealthCheck) {
                firstTimeHealthCheck = false;
                new Thread(() -> {
                    GatewayKitService gatewayKitService = getGatewayKitService(false);
                    gatewayKitService.start();
                }).start();
            }

            return healthy;
        } catch (Exception ex) {
            LOGGER.error("Unhealthy", ex);
            return unhealthy;
        }
    }

    @RequestMapping(value = "/counters.html", method = RequestMethod.GET)
    @ApiIgnore
    public String getCountersHtml() {
        return EndpointUtils.getCountersHtmlDoc(EndpointUtils.getApplicationName());
    }

    @RequestMapping(value = "/reset-counters", method = RequestMethod.GET)
    @ApiIgnore
    public void resetCounters() {
        LOGGER.info("Endpoint: reset-counters");
        PerfCounter.resetAllCounters();
    }

    @RequestMapping(value = "/terminate", method = RequestMethod.GET)
    @ApiIgnore
    public void terminate(
            HttpServletResponse response
            ) {
        LOGGER.info("Endpoint: terminate");
        new Thread(() -> System.exit(0)).start();

        if (response != null) {
            try {
                response.sendRedirect("/counters.html");
            } catch (IOException e) {
                // Eat it;
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Non-standard functions
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @RequestMapping(value = "/**")
    public void anyMethod(
            HttpServletResponse response,
            HttpServletRequest request) {
        LOGGER.info(String.format("Endpoint %s: %s", request.getMethod(), request.getRequestURI()));

        getGatewayKitService(true).routeRequest(request, response);
    }

}

