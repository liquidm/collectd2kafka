package org.collectd.collectd2kafka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.serializer.StringEncoder;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.collectd.api.Collectd;
import org.collectd.api.CollectdConfigInterface;
import org.collectd.api.CollectdReadInterface;
import org.collectd.api.CollectdShutdownInterface;
import org.collectd.api.CollectdWriteInterface;
import org.collectd.api.DataSource;
import org.collectd.api.OConfigItem;
import org.collectd.api.ValueList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Collectd2KafkaPlugin implements CollectdConfigInterface,
                                        CollectdReadInterface,
                                        CollectdWriteInterface,
                                        CollectdShutdownInterface {

    private final Logger logger = LoggerFactory.getLogger(Collectd2KafkaPlugin.class);

    private static final String __DEFAULT_BROKER_LIST = "localhost:9092";
    private static final String __DEFAULT_TOPIC = "collectd";
    private String kafkaProducerTopic = __DEFAULT_TOPIC;
    public static Properties kafkaProducerProperties = null;
    private Producer<String, String> kafkaMessageProducer = null;

    public Collectd2KafkaPlugin(){
      Collectd.registerConfig   ("Collectd2KafkaPlugin", this);
      Collectd.registerRead     ("Collectd2KafkaPlugin", this);
      Collectd.registerWrite("Collectd2KafkaPlugin", this);
      Collectd.registerShutdown ("Collectd2KafkaPlugin", this);

      kafkaProducerProperties = kafkaProducerDefaultProperties();
    }

    public int config(OConfigItem ci) {
        logger.info("Collectd2KafkaPlugin::config lifecycle", this);
        loadCollectd2KafkaPluginConfigProperties(ci);
        //Collectd.logInfo("Collectd2KafkaPlugin::config lifecycle");
        return 0;
    }

    public int write(ValueList vl) {
        logger.info("Collectd2KafkaPlugin::write lifecycle", this);
        //Collectd.logInfo("Collectd2KafkaPlugin::write lifecycle");

        Map<String, String> cdOut = collectd2kafkaMessageBuilder(vl);

        JsonNode json = new ObjectMapper().valueToTree(cdOut);
        String jsonPayload = json.toString();
        KeyedMessage<String, String> payload = new KeyedMessage<String, String>(kafkaProducerProperties.getProperty("producer.topic"), jsonPayload);

        if (null != getKafkaProducer()) {
            getKafkaProducer().send(payload);
        } else {
            logger.warn(
                    "Could not obtain kafka Producer discarding payload: %s for topic: %s"
                            + payload.message(), payload.topic());
        }

        return Collectd.dispatchValues (vl);

    }

    public int read() {
        logger.info("Collectd2KafkaPlugin::read lifecycle", this);
        //Collectd.logInfo("Collectd2KafkaPlugin::read lifecycle");
        return 0;
    }

    public int shutdown() {
        logger.info("Collectd2KafkaPlugin::shutdown lifecycle", this);
        if(null != kafkaMessageProducer) {
            try {
                kafkaMessageProducer.close();
            }catch(Exception e){
                logger.warn("exception with kafka producer client shutdown", e);
            }
        }
        //Collectd.logInfo("Collectd2KafkaPlugin::shutdown lifecycle");
        return 0;
    }

    protected Producer<String, String> getKafkaProducer() {
        try {
            if (null == kafkaMessageProducer) {
                kafkaMessageProducer = new Producer<String, String>(getProducerConfig(kafkaProducerProperties));
            }
        } catch (Exception e) {
            logger.warn("Error getting kafkaProducer", e);
        }
        return kafkaMessageProducer;
    }

    protected Producer<String, String> getKafkaProducer(Properties props) {
        try {
            if (null == kafkaMessageProducer) {
                kafkaMessageProducer = new Producer<String, String>(getProducerConfig(props));
            }
        } catch (Exception e) {
            logger.warn("Error getting kafkaProducer", e);
        }
        return kafkaMessageProducer;
    }

    protected ProducerConfig getProducerConfig(Properties props) {
        ProducerConfig config = new ProducerConfig(props);
        return config;
    }

    protected static Properties kafkaProducerDefaultProperties(){
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
        props.put("metadata.broker.list", __DEFAULT_BROKER_LIST);
        return props;
    }
    
    protected void loadCollectd2KafkaPluginConfigProperties(OConfigItem ci) {
        for (OConfigItem item : ci.getChildren()) {
            final String key = item.getKey();
            if (key.equalsIgnoreCase("metadata.broker.list")) {
                if (null != item.getValues() && item.getValues().size() > 0) {
                    String kafkaBrokerList = item.getValues().get(0).getString();
                    kafkaProducerProperties.put("metadata.broker.list", kafkaBrokerList);
                }
                else {
                    logger.info("using default value for %s: %s", 
                            key,
                            __DEFAULT_BROKER_LIST);
                }
              
            } else if (key.equalsIgnoreCase("producer.topic")) {
                if (null != item.getValues() && item.getValues().size() > 0) {
                    kafkaProducerTopic = item.getValues().get(0).getString();
                    kafkaProducerProperties.put("producer.topic", kafkaProducerTopic);
                }
                else {
                    logger.info("using default value for %s: %s",
                            key,
                            __DEFAULT_TOPIC);
                }
            }
        }

    }

    private Map<String, String> collectd2kafkaMessageBuilder(ValueList vl) {
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
        return cdOut;
    }

}