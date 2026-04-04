package com.tactical.backend.tactical_host;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.tactical.backend.commons.GlobalVariables;
import com.tactical.backend.commons.PerfCounter;
import com.tactical.backend.commons.utils.ResourceUtils;
import com.tactical.backend.model.AwsEnvironment;
import com.tactical.backend.tactical_host.model.ConfigYaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;

public class GatewayKitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayKitService.class.getSimpleName());

    private static final int DEFAULT_BUFFER_SIZE = 4096;

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

    public void loadGatewayYamlFile(String filename) {
        String yamlContent = ResourceUtils.getInstance().load(filename);

        try {
            configYaml = mapper.readValue(yamlContent, ConfigYaml.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void routeRequest(HttpServletRequest req, HttpServletResponse resp) {
        // Find the corresponding Route
        ConfigYaml.Route route = configYaml.findRoute(req.getRequestURI());
        if (route == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Make sure method matches
        boolean methodFound = false;
        for (String method : route.methods) {
            if (method.equalsIgnoreCase(req.getMethod())) {
                methodFound = true;
                break;
            }
        }

        if (!methodFound) {
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String targetUrl = route.upstream.url + route.path;

        PerfCounter perfCounter = PerfCounter.get(getClass().getSimpleName(), req.getRequestURI()).start();
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Copy HTTP method
            conn.setRequestMethod(req.getMethod());

            // Copy headers
            Collections.list(req.getHeaderNames()).forEach(headerName -> {
                Collections.list(req.getHeaders(headerName)).forEach(value -> {
                    conn.addRequestProperty(headerName, value);
                });
            });

            // Send request body if present
            conn.setDoOutput(true);
            if (req.getInputStream() != null && req.getContentLength() > 0) {
                try (OutputStream os = conn.getOutputStream();
                     InputStream is = req.getInputStream()) {
                    transfer(is, os);
                }
            }

            int status = conn.getResponseCode();
            resp.setStatus(status);

            // Copy response headers
            conn.getHeaderFields().forEach((key, values) -> {
                if (key != null) {
                    for (String value : values) {
                        resp.addHeader(key, value);
                    }
                }
            });

            // Copy response body
            InputStream responseStream = (status >= 400)
                    ? conn.getErrorStream()
                    : conn.getInputStream();

            if (responseStream != null) {
                try (OutputStream out = resp.getOutputStream()) {
                    transfer(responseStream, out);
                }
            }
        } catch (Exception e) {
            perfCounter.failAndStop();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (OutputStream out = resp.getOutputStream()) {
                DataOutputStream dos = new DataOutputStream(out);
                dos.writeUTF(e.getMessage());
                dos.flush();
            } catch (Exception ex2) {
                // eat it
            }
        } finally {
            perfCounter.incrementAndStop();
        }
    }

    private long transfer(InputStream is, OutputStream out) throws IOException {
        long transferred = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = is.read(buffer, 0, buffer.length)) >= 0) {
            out.write(buffer, 0, read);
            transferred += read;
        }
        return transferred;
    }


}