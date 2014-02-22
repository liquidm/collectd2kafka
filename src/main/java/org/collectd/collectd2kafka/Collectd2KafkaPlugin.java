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
    private final String __DEAFULT_TOPIC = "collectd";

    private String zookeeperHostAndPost = __DEFAULT_HOST_PORT;
    private String kafkaProducerTopic = __DEAFULT_TOPIC;
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
        KeyedMessage<String, String> payload = new KeyedMessage<String, String>(
                kafkaProducerTopic, jsonPayload);

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
                ProducerConfig config = buildDefaultProducerConfig();
                kafkaMessageProducer = new Producer<String, String>(config);
            }

        } catch (Exception e) {
            logger.warn("Error getting kafkaProducer", e);
        }
        return kafkaMessageProducer;
    }

    protected ProducerConfig buildDefaultProducerConfig() {
        Properties props = new Properties();
        props.put("zk.connect", zookeeperHostAndPost);
        props.put("serializer.class", StringEncoder.class.getName()); // "kafka.serializer.StringEncoder"
        //FIXME: init this property... is required!!! .. tests expose that
        props.put("metadata.broker.list", "");
        ProducerConfig config = new ProducerConfig(props);
        return config;
    }

    protected void loadCollectd2KafkaPluginConfigProperties(OConfigItem ci) {
        for (OConfigItem item : ci.getChildren()) {
            final String key = item.getKey();
            if (key.equalsIgnoreCase("zk.connect")) {
                if (null != item.getValues() && item.getValues().size() > 0) {
                    zookeeperHostAndPost = item.getValues().get(0).getString();
                }
                else {
                    logger.info("using default value for %s: %s", 
                            key,
                            zookeeperHostAndPost);
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