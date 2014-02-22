package org.collectd.collectd2kafka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.serializer.StringEncoder;

import org.collectd.api.Collectd;
import org.collectd.api.CollectdConfigInterface;
import org.collectd.api.CollectdInitInterface;
import org.collectd.api.CollectdReadInterface;
import org.collectd.api.CollectdShutdownInterface;
import org.collectd.api.CollectdWriteInterface;
import org.collectd.api.DataSource;
import org.collectd.api.OConfigItem;
import org.collectd.api.ValueList;

public class Collectd2KafkaPlugin implements CollectdConfigInterface,
        CollectdInitInterface, CollectdReadInterface, CollectdWriteInterface,
        CollectdShutdownInterface {

    private final Logger logger = LoggerFactory.getLogger(Collectd2KafkaPlugin.class);
    private final String __DEFAULT_HOST_PORT = "localhost:2181";
    private final String __DEFAULT_BROKER_LIST =__DEFAULT_HOST_PORT; //FIXME: check this value
    private final String __DEFAULT_TOPIC = "collectd";

    private String zookeeperHostAndPost = __DEFAULT_HOST_PORT;
    private String kafkaProducerTopic = __DEFAULT_TOPIC;
    private String kafkaBrokerList = __DEFAULT_BROKER_LIST;
    private Producer<String, String> kafkaMessageProducer = null;

    public Collectd2KafkaPlugin() {
    }

    public int config(OConfigItem ci) {
        logger.info("Collectd2KafkaPlugin::config lifecycle", this);
        Collectd.logInfo("Collectd2KafkaPlugin::config lifecycle");
        loadCollectd2KafkaPluginConfigProperties(ci);
        return 0;
    }

    public int init() {
        logger.info("Collectd2KafkaPlugin::init starts.. registering lifecycle interfaces", this);
        Collectd.logInfo("Collectd2KafkaPlugin::init starts.. registering lifecycle interfaces");
        
        Collectd.registerConfig(CollectdConfigInterface.class.getSimpleName(), this);
        Collectd.registerInit(CollectdInitInterface.class.getSimpleName(), this);
        Collectd.registerRead(CollectdReadInterface.class.getSimpleName(), this);
        Collectd.registerWrite(CollectdWriteInterface.class.getSimpleName(), this);
        Collectd.registerShutdown(CollectdShutdownInterface.class.getSimpleName(), this);
        
        logger.info("Collectd2KafkaPlugin::init ends.. registering lifecycle interfaces done!!", this);
        Collectd.logInfo("Collectd2KafkaPlugin::init ends.. registering lifecycle interfaces done!!");
        
        return 0;
    }

    public int write(ValueList vl) {
        logger.info("Collectd2KafkaPlugin::write lifecycle", this);
        Collectd.logInfo("Collectd2KafkaPlugin::write lifecycle");

        List<DataSource> ds = vl.getDataSet().getDataSources();
        List<Number> values = vl.getValues();
        int size = values.size();
        String plugin = vl.getPlugin();
        String type = vl.getType();
        String pluginInstance = vl.getPluginInstance();
        String typeInstance = vl.getTypeInstance();

        Map<String, String> cdOut = new HashMap<String, String>();
        if (plugin != null && !plugin.isEmpty()) {
            cdOut.put("plugin", plugin);
            if (pluginInstance != null && !pluginInstance.isEmpty())
                cdOut.put("instance", pluginInstance);
            if (type != null && !type.isEmpty())
                cdOut.put("type", type);
            if (typeInstance != null && !typeInstance.isEmpty())
                cdOut.put("type_instance", typeInstance);
        }
        long time = vl.getTime() / 1000L;
        cdOut.put("time", String.valueOf(time));
        cdOut.put("host", vl.getHost());
        for (int x = 0; x < size; x++) {
            String pointName = ((DataSource) ds.get(x)).getName();
            if (!pointName.equals("value"))
                cdOut.put("value_name", pointName);
        }

        JsonNode json = new ObjectMapper().valueToTree(cdOut);
        String jsonPayload = json.toString();
        KeyedMessage<String, String> payload = new KeyedMessage<String, String>(kafkaProducerTopic, jsonPayload);

        if (null != getKafkaProducer()) {
            getKafkaProducer().send(payload);
        } else {
            logger.warn(
                    "Could not obtain kafka Producer discarding payload: %s for topic: %s"
                            + payload.message(), payload.topic());
        }

        return 0;

    }

    public int read() {
        logger.info("Collectd2KafkaPlugin::read lifecycle", this);
        Collectd.logInfo("Collectd2KafkaPlugin::read lifecycle");
        return 0;
    }

    public int shutdown() {
        logger.info("Collectd2KafkaPlugin::shutdown lifecycle", this);
        Collectd.logInfo("Collectd2KafkaPlugin::shutdown lifecycle");
        return 0;
    }

    protected Producer<String, String> getKafkaProducer() {
        try {
            if (null == kafkaMessageProducer) {
                kafkaMessageProducer = new Producer<String, String>(getProducerConfig());
            }

        } catch (Exception e) {
            logger.warn("Error getting kafkaProducer", e);
        }
        return kafkaMessageProducer;
    }

    protected ProducerConfig getProducerConfig() {
        ProducerConfig config = new ProducerConfig(buildProducerProperties());
        return config;
    }

    protected Properties buildProducerProperties(){
        Properties props = new Properties();
        // see http://kafka.apache.org/08/configuration.html
        /*
            metadata.broker.list    => no default (required)
            request.required.acks   => default to 0
            producer.type   => default to sync
            serializer.class => default to kafka.serializer.DefaultEncoder The serializer class for messages. The default encoder takes a byte[] and returns the same byte[].
            client.id => defaults to ""  The client id is a user-specified string sent in each request to help trace calls. 
                         It should logically identify the application making the request.

         */
        props.put("client.id", Collectd2KafkaPlugin.class.getName()); //TODO: add host information addition in order to identify host as well
        props.put("serializer.class", StringEncoder.class.getName()); // "kafka.serializer.StringEncoder"
        props.put("metadata.broker.list", kafkaBrokerList);
          
        //props.put("zk.connect", zookeeperHostAndPost); // LOOKS THAT IS ONLY FOR CONSUMERS 
       
        return props;
    }
    
    protected void loadCollectd2KafkaPluginConfigProperties(OConfigItem ci) {
        for (OConfigItem item : ci.getChildren()) {
            final String key = item.getKey();
            if (key.equalsIgnoreCase("metadata.broker.list")) {
                if (null != item.getValues() && item.getValues().size() > 0) {
                    zookeeperHostAndPost = item.getValues().get(0).getString();
                }
                else {
                    logger.info("using default value for %s: %s", 
                            key,
                            kafkaBrokerList);
                }
              
            } else if (key.equalsIgnoreCase("producer.topic")) {
                if (null != item.getValues() && item.getValues().size() > 0) {
                    kafkaProducerTopic = item.getValues().get(0).getString();
                }
                else {
                    logger.info("using default value for %s: %s",
                            key,
                            kafkaProducerTopic);
                }
            }
        }

    }

}