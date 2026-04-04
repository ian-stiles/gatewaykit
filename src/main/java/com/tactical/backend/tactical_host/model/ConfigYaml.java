package com.tactical.backend.tactical_host.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigYaml {

    public Gateway gateway;
    public List<Route> routes;

    private Map<String, Route> routeMap;

    public Route findRoute(String path) {
        if (routeMap == null) {
            synchronized (routes) {
                routeMap = new HashMap<>();
                for (Route route : routes) {
                    routeMap.put(route.path, route);
                }
            }
        }

        return routeMap.get(path);
    }

    public static class Gateway {
        public int port;
        public String global_timeout;
        public RateLimit global_rate_limit;
    }

    public static class Route {
        public String path;
        public List<String> methods;
        public boolean strip_prefix;

        public Upstream upstream;
        public String timeout;

        public RateLimit rate_limit;
        public Retry retry;

        public RequestTransform request_transform;
        public ResponseTransform response_transform;

        public Auth auth;
        public CircuitBreaker circuit_breaker;
    }

    // ---------- Upstream ----------

    public static class Upstream {
        public String url;

        public List<Target> targets;
        public String balance;
        public String timeout;

        public HealthCheck health_check;
    }

    public static class Target {
        public String url;
        public int weight;
    }

    public static class HealthCheck {
        public String path;
        public String interval;
        public int unhealthy_threshold;
    }

    // ---------- Rate Limiting ----------

    public static class RateLimit {
        public int requests;
        public String window;
        public String strategy;
        public String per;
    }

    // ---------- Retry ----------

    public static class Retry {
        public int attempts;
        public String backoff;
        public String initial_delay;
        public List<Integer> on;
    }

    // ---------- Auth ----------

    public static class Auth {
        public String type;
        public String header;
        public List<String> keys;
    }

    // ---------- Circuit Breaker ----------

    public static class CircuitBreaker {
        public int threshold;
        public String window;
        public String cooldown;
    }

    // ---------- Request Transform ----------

    public static class RequestTransform {
        public HeaderTransform headers;
        public BodyTransform body;
    }

    public static class ResponseTransform {
        public HeaderTransform headers;
        public ResponseBodyTransform body;
    }

    public static class HeaderTransform {
        public Map<String, String> add;
        public List<String> remove;
    }

    public static class BodyTransform {
        public Map<String, String> mapping;
    }

    public static class ResponseBodyTransform {
        public Envelope envelope;
    }

    public static class Envelope {
        public Object data; // "$body"
        public Map<String, String> gateway_metadata;
    }
}
