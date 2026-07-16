package com.ngs.analytics.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ngs")
public class NgsProperties {

    private final Jwt jwt = new Jwt();
    private final Storage storage = new Storage();
    private final Queue queue = new Queue();
    private final Spark spark = new Spark();
    private final Minio minio = new Minio();

    public Jwt getJwt() {
        return jwt;
    }

    public Storage getStorage() {
        return storage;
    }

    public Queue getQueue() {
        return queue;
    }

    public Spark getSpark() {
        return spark;
    }

    public Minio getMinio() {
        return minio;
    }

    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }

    public static class Storage {
        private String type = "local";
        private String localDir = "../data/uploads";
        private long maxUploadBytes = 209715200L;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLocalDir() {
            return localDir;
        }

        public void setLocalDir(String localDir) {
            this.localDir = localDir;
        }

        public long getMaxUploadBytes() {
            return maxUploadBytes;
        }

        public void setMaxUploadBytes(long maxUploadBytes) {
            this.maxUploadBytes = maxUploadBytes;
        }
    }

    public static class Queue {
        private boolean enabled;
        private String exchange = "ngs.analysis";
        private String routingKey = "analysis.jobs";
        private String queue = "ngs.analysis.jobs";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }
    }

    public static class Spark {
        private boolean enabled;
        private String master = "local[2]";
        private String driverMemory = "1g";
        private String jarPath = "../spark-jobs/target/ngs-spark-jobs-0.1.0-SNAPSHOT.jar";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMaster() {
            return master;
        }

        public void setMaster(String master) {
            this.master = master;
        }

        public String getDriverMemory() {
            return driverMemory;
        }

        public void setDriverMemory(String driverMemory) {
            this.driverMemory = driverMemory;
        }

        public String getJarPath() {
            return jarPath;
        }

        public void setJarPath(String jarPath) {
            this.jarPath = jarPath;
        }
    }

    public static class Minio {
        private boolean enabled;
        private String endpoint = "http://localhost:9000";
        private String accessKey = "ngsminio";
        private String secretKey = "ngsminio123";
        private String bucket = "ngs-uploads";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }
}
