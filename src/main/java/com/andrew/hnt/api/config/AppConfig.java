package com.andrew.hnt.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

/**
 * 애플리케이션 설정 관리 클래스
 * HnT Sensor API 프로젝트
 */
@Configuration
@PropertySource("classpath:application-settings.yml")
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private Mqtt mqtt = new Mqtt();
    private Database database = new Database();
    private Sensor sensor = new Sensor();
    private Notification notification = new Notification();
    private Security security = new Security();
    private Logging logging = new Logging();
    private Ui ui = new Ui();
    private Performance performance = new Performance();
    private Development development = new Development();
    
    // Getters and Setters
    public Mqtt getMqtt() { return mqtt; }
    public void setMqtt(Mqtt mqtt) { this.mqtt = mqtt; }
    
    public Database getDatabase() { return database; }
    public void setDatabase(Database database) { this.database = database; }
    
    public Sensor getSensor() { return sensor; }
    public void setSensor(Sensor sensor) { this.sensor = sensor; }
    
    public Notification getNotification() { return notification; }
    public void setNotification(Notification notification) { this.notification = notification; }
    
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
    
    public Logging getLogging() { return logging; }
    public void setLogging(Logging logging) { this.logging = logging; }
    
    public Ui getUi() { return ui; }
    public void setUi(Ui ui) { this.ui = ui; }
    
    public Performance getPerformance() { return performance; }
    public void setPerformance(Performance performance) { this.performance = performance; }
    
    public Development getDevelopment() { return development; }
    public void setDevelopment(Development development) { this.development = development; }
    
    // Inner Classes
    public static class Mqtt {
        private Broker broker = new Broker();
        private Topic topic = new Topic();
        private Message message = new Message();
        
        public Broker getBroker() { return broker; }
        public void setBroker(Broker broker) { this.broker = broker; }
        
        public Topic getTopic() { return topic; }
        public void setTopic(Topic topic) { this.topic = topic; }
        
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        
        public static class Broker {
            private String host;
            private int port;
            private String username;
            private String password;
            private String clientIdPrefix;
            private int keepAlive;
            private int timeout;
            private int reconnectDelay;
            private int maxReconnectAttempts;
            
            // Getters and Setters
            public String getHost() { return host; }
            public void setHost(String host) { this.host = host; }
            
            public int getPort() { return port; }
            public void setPort(int port) { this.port = port; }
            
            public String getUsername() { return username; }
            public void setUsername(String username) { this.username = username; }
            
            public String getPassword() { return password; }
            public void setPassword(String password) { this.password = password; }
            
            public String getClientIdPrefix() { return clientIdPrefix; }
            public void setClientIdPrefix(String clientIdPrefix) { this.clientIdPrefix = clientIdPrefix; }
            
            public int getKeepAlive() { return keepAlive; }
            public void setKeepAlive(int keepAlive) { this.keepAlive = keepAlive; }
            
            public int getTimeout() { return timeout; }
            public void setTimeout(int timeout) { this.timeout = timeout; }
            
            public int getReconnectDelay() { return reconnectDelay; }
            public void setReconnectDelay(int reconnectDelay) { this.reconnectDelay = reconnectDelay; }
            
            public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
            public void setMaxReconnectAttempts(int maxReconnectAttempts) { this.maxReconnectAttempts = maxReconnectAttempts; }
        }
        
        public static class Topic {
            private String pattern;
            private List<String> types;
            
            public String getPattern() { return pattern; }
            public void setPattern(String pattern) { this.pattern = pattern; }
            
            public List<String> getTypes() { return types; }
            public void setTypes(List<String> types) { this.types = types; }
        }
        
        public static class Message {
            private int maxLength;
            private boolean validationEnabled;
            
            public int getMaxLength() { return maxLength; }
            public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
            
            public boolean isValidationEnabled() { return validationEnabled; }
            public void setValidationEnabled(boolean validationEnabled) { this.validationEnabled = validationEnabled; }
        }
    }
    
    public static class Database {
        private Connection connection = new Connection();
        private Pool pool = new Pool();
        
        public Connection getConnection() { return connection; }
        public void setConnection(Connection connection) { this.connection = connection; }
        
        public Pool getPool() { return pool; }
        public void setPool(Pool pool) { this.pool = pool; }
        
        public static class Connection {
            private String url;
            private String username;
            private String password;
            private String driverClass;
            
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
            
            public String getUsername() { return username; }
            public void setUsername(String username) { this.username = username; }
            
            public String getPassword() { return password; }
            public void setPassword(String password) { this.password = password; }
            
            public String getDriverClass() { return driverClass; }
            public void setDriverClass(String driverClass) { this.driverClass = driverClass; }
        }
        
        public static class Pool {
            private long maxLifetime;
            private int connectionTimeout;
            private int minimumIdle;
            private int maximumPoolSize;
            private long idleTimeout;
            private long leakDetectionThreshold;
            
            public long getMaxLifetime() { return maxLifetime; }
            public void setMaxLifetime(long maxLifetime) { this.maxLifetime = maxLifetime; }
            
            public int getConnectionTimeout() { return connectionTimeout; }
            public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
            
            public int getMinimumIdle() { return minimumIdle; }
            public void setMinimumIdle(int minimumIdle) { this.minimumIdle = minimumIdle; }
            
            public int getMaximumPoolSize() { return maximumPoolSize; }
            public void setMaximumPoolSize(int maximumPoolSize) { this.maximumPoolSize = maximumPoolSize; }
            
            public long getIdleTimeout() { return idleTimeout; }
            public void setIdleTimeout(long idleTimeout) { this.idleTimeout = idleTimeout; }
            
            public long getLeakDetectionThreshold() { return leakDetectionThreshold; }
            public void setLeakDetectionThreshold(long leakDetectionThreshold) { this.leakDetectionThreshold = leakDetectionThreshold; }
        }
    }
    
    public static class Sensor {
        private Types types = new Types();
        private Parameters parameters = new Parameters();
        private Validation validation = new Validation();
        
        public Types getTypes() { return types; }
        public void setTypes(Types types) { this.types = types; }
        
        public Parameters getParameters() { return parameters; }
        public void setParameters(Parameters parameters) { this.parameters = parameters; }
        
        public Validation getValidation() { return validation; }
        public void setValidation(Validation validation) { this.validation = validation; }
        
        public static class Types {
            private List<String> allowed;
            
            public List<String> getAllowed() { return allowed; }
            public void setAllowed(List<String> allowed) { this.allowed = allowed; }
        }
        
        public static class Parameters {
            private int count;
            private Range range = new Range();
            
            public int getCount() { return count; }
            public void setCount(int count) { this.count = count; }
            
            public Range getRange() { return range; }
            public void setRange(Range range) { this.range = range; }
            
            public static class Range {
                private int min;
                private int max;
                
                public int getMin() { return min; }
                public void setMin(int min) { this.min = min; }
                
                public int getMax() { return max; }
                public void setMax(int max) { this.max = max; }
            }
        }
        
        public static class Validation {
            private Temperature temperature = new Temperature();
            private Value value = new Value();
            
            public Temperature getTemperature() { return temperature; }
            public void setTemperature(Temperature temperature) { this.temperature = temperature; }
            
            public Value getValue() { return value; }
            public void setValue(Value value) { this.value = value; }
            
            public static class Temperature {
                private int min;
                private int max;
                
                public int getMin() { return min; }
                public void setMin(int min) { this.min = min; }
                
                public int getMax() { return max; }
                public void setMax(int max) { this.max = max; }
            }
            
            public static class Value {
                private int min;
                private int max;
                
                public int getMin() { return min; }
                public void setMin(int min) { this.min = min; }
                
                public int getMax() { return max; }
                public void setMax(int max) { this.max = max; }
            }
        }
    }
    
    public static class Notification {
        private Fcm fcm = new Fcm();
        private List<String> types;
        
        public Fcm getFcm() { return fcm; }
        public void setFcm(Fcm fcm) { this.fcm = fcm; }
        
        public List<String> getTypes() { return types; }
        public void setTypes(List<String> types) { this.types = types; }
        
        public static class Fcm {
            private String apiKey;
            private String senderId;
            
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            
            public String getSenderId() { return senderId; }
            public void setSenderId(String senderId) { this.senderId = senderId; }
        }
    }
    
    public static class Security {
        private Session session = new Session();
        private Password password = new Password();
        private Validation validation = new Validation();
        
        public Session getSession() { return session; }
        public void setSession(Session session) { this.session = session; }
        
        public Password getPassword() { return password; }
        public void setPassword(Password password) { this.password = password; }
        
        public Validation getValidation() { return validation; }
        public void setValidation(Validation validation) { this.validation = validation; }
        
        public static class Session {
            private int timeout;
            private int maxInactiveInterval;
            
            public int getTimeout() { return timeout; }
            public void setTimeout(int timeout) { this.timeout = timeout; }
            
            public int getMaxInactiveInterval() { return maxInactiveInterval; }
            public void setMaxInactiveInterval(int maxInactiveInterval) { this.maxInactiveInterval = maxInactiveInterval; }
        }
        
        public static class Password {
            private Encryption encryption = new Encryption();
            
            public Encryption getEncryption() { return encryption; }
            public void setEncryption(Encryption encryption) { this.encryption = encryption; }
            
            public static class Encryption {
                private String algorithm;
                private int keyLength;
                
                public String getAlgorithm() { return algorithm; }
                public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
                
                public int getKeyLength() { return keyLength; }
                public void setKeyLength(int keyLength) { this.keyLength = keyLength; }
            }
        }
        
        public static class Validation {
            private UserId userId = new UserId();
            private Uuid uuid = new Uuid();
            
            public UserId getUserId() { return userId; }
            public void setUserId(UserId userId) { this.userId = userId; }
            
            public Uuid getUuid() { return uuid; }
            public void setUuid(Uuid uuid) { this.uuid = uuid; }
            
            public static class UserId {
                private int minLength;
                private int maxLength;
                private String pattern;
                
                public int getMinLength() { return minLength; }
                public void setMinLength(int minLength) { this.minLength = minLength; }
                
                public int getMaxLength() { return maxLength; }
                public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
                
                public String getPattern() { return pattern; }
                public void setPattern(String pattern) { this.pattern = pattern; }
            }
            
            public static class Uuid {
                private int minLength;
                private int maxLength;
                private String pattern;
                
                public int getMinLength() { return minLength; }
                public void setMinLength(int minLength) { this.minLength = minLength; }
                
                public int getMaxLength() { return maxLength; }
                public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
                
                public String getPattern() { return pattern; }
                public void setPattern(String pattern) { this.pattern = pattern; }
            }
        }
    }
    
    public static class Logging {
        private Filter filter = new Filter();
        private Console console = new Console();
        private File file = new File();
        
        public Filter getFilter() { return filter; }
        public void setFilter(Filter filter) { this.filter = filter; }
        
        public Console getConsole() { return console; }
        public void setConsole(Console console) { this.console = console; }
        
        public File getFile() { return file; }
        public void setFile(File file) { this.file = file; }
        
        public static class Filter {
            private int maxLogsPerMinute;
            private long resetInterval;
            
            public int getMaxLogsPerMinute() { return maxLogsPerMinute; }
            public void setMaxLogsPerMinute(int maxLogsPerMinute) { this.maxLogsPerMinute = maxLogsPerMinute; }
            
            public long getResetInterval() { return resetInterval; }
            public void setResetInterval(long resetInterval) { this.resetInterval = resetInterval; }
        }
        
        public static class Console {
            private boolean enabled;
            private String level;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public String getLevel() { return level; }
            public void setLevel(String level) { this.level = level; }
        }
        
        public static class File {
            private boolean enabled;
            private String level;
            private String path;
            private String maxSize;
            private int maxFiles;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public String getLevel() { return level; }
            public void setLevel(String level) { this.level = level; }
            
            public String getPath() { return path; }
            public void setPath(String path) { this.path = path; }
            
            public String getMaxSize() { return maxSize; }
            public void setMaxSize(String maxSize) { this.maxSize = maxSize; }
            
            public int getMaxFiles() { return maxFiles; }
            public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }
        }
    }
    
    public static class Ui {
        private Buttons buttons = new Buttons();
        private Alerts alerts = new Alerts();
        private StatusIndicators statusIndicators = new StatusIndicators();
        
        public Buttons getButtons() { return buttons; }
        public void setButtons(Buttons buttons) { this.buttons = buttons; }
        
        public Alerts getAlerts() { return alerts; }
        public void setAlerts(Alerts alerts) { this.alerts = alerts; }
        
        public StatusIndicators getStatusIndicators() { return statusIndicators; }
        public void setStatusIndicators(StatusIndicators statusIndicators) { this.statusIndicators = statusIndicators; }
        
        public static class Buttons {
            private List<String> styles;
            private List<String> sizes;
            
            public List<String> getStyles() { return styles; }
            public void setStyles(List<String> styles) { this.styles = styles; }
            
            public List<String> getSizes() { return sizes; }
            public void setSizes(List<String> sizes) { this.sizes = sizes; }
        }
        
        public static class Alerts {
            private Duration duration = new Duration();
            
            public Duration getDuration() { return duration; }
            public void setDuration(Duration duration) { this.duration = duration; }
            
            public static class Duration {
                private int success;
                private int error;
                private int warning;
                private int info;
                
                public int getSuccess() { return success; }
                public void setSuccess(int success) { this.success = success; }
                
                public int getError() { return error; }
                public void setError(int error) { this.error = error; }
                
                public int getWarning() { return warning; }
                public void setWarning(int warning) { this.warning = warning; }
                
                public int getInfo() { return info; }
                public void setInfo(int info) { this.info = info; }
            }
        }
        
        public static class StatusIndicators {
            private List<String> colors;
            private Animation animation = new Animation();
            
            public List<String> getColors() { return colors; }
            public void setColors(List<String> colors) { this.colors = colors; }
            
            public Animation getAnimation() { return animation; }
            public void setAnimation(Animation animation) { this.animation = animation; }
            
            public static class Animation {
                private int pulseDuration;
                
                public int getPulseDuration() { return pulseDuration; }
                public void setPulseDuration(int pulseDuration) { this.pulseDuration = pulseDuration; }
            }
        }
    }
    
    public static class Performance {
        private Mqtt mqtt = new Mqtt();
        private Database database = new Database();
        private Cache cache = new Cache();
        
        public Mqtt getMqtt() { return mqtt; }
        public void setMqtt(Mqtt mqtt) { this.mqtt = mqtt; }
        
        public Database getDatabase() { return database; }
        public void setDatabase(Database database) { this.database = database; }
        
        public Cache getCache() { return cache; }
        public void setCache(Cache cache) { this.cache = cache; }
        
        public static class Mqtt {
            private int connectionPoolSize;
            private int messageBatchSize;
            
            public int getConnectionPoolSize() { return connectionPoolSize; }
            public void setConnectionPoolSize(int connectionPoolSize) { this.connectionPoolSize = connectionPoolSize; }
            
            public int getMessageBatchSize() { return messageBatchSize; }
            public void setMessageBatchSize(int messageBatchSize) { this.messageBatchSize = messageBatchSize; }
        }
        
        public static class Database {
            private int queryTimeout;
            private int batchSize;
            
            public int getQueryTimeout() { return queryTimeout; }
            public void setQueryTimeout(int queryTimeout) { this.queryTimeout = queryTimeout; }
            
            public int getBatchSize() { return batchSize; }
            public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        }
        
        public static class Cache {
            private boolean enabled;
            private int ttl;
            private int maxSize;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public int getTtl() { return ttl; }
            public void setTtl(int ttl) { this.ttl = ttl; }
            
            public int getMaxSize() { return maxSize; }
            public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
        }
    }
    
    public static class Development {
        private Debug debug = new Debug();
        private Features features = new Features();
        
        public Debug getDebug() { return debug; }
        public void setDebug(Debug debug) { this.debug = debug; }
        
        public Features getFeatures() { return features; }
        public void setFeatures(Features features) { this.features = features; }
        
        public static class Debug {
            private boolean enabled;
            private boolean mqttLogs;
            private boolean performanceLogs;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public boolean isMqttLogs() { return mqttLogs; }
            public void setMqttLogs(boolean mqttLogs) { this.mqttLogs = mqttLogs; }
            
            public boolean isPerformanceLogs() { return performanceLogs; }
            public void setPerformanceLogs(boolean performanceLogs) { this.performanceLogs = performanceLogs; }
        }
        
        public static class Features {
            private boolean autoReconnect;
            private boolean errorRecovery;
            private boolean dataValidation;
            
            public boolean isAutoReconnect() { return autoReconnect; }
            public void setAutoReconnect(boolean autoReconnect) { this.autoReconnect = autoReconnect; }
            
            public boolean isErrorRecovery() { return errorRecovery; }
            public void setErrorRecovery(boolean errorRecovery) { this.errorRecovery = errorRecovery; }
            
            public boolean isDataValidation() { return dataValidation; }
            public void setDataValidation(boolean dataValidation) { this.dataValidation = dataValidation; }
        }
    }
}
