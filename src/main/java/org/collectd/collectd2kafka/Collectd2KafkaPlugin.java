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
import org.collectd.api.CollectdInitInterface;
import org.collectd.api.CollectdReadInterface;
import org.collectd.api.CollectdShutdownInterface;
import org.collectd.api.CollectdWriteInterface;
import org.collectd.api.DataSource;
import org.collectd.api.OConfigItem;
import org.collectd.api.ValueList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Collectd2KafkaPlugin implements CollectdConfigInterface, CollectdInitInterface, CollectdReadInterface, CollectdWriteInterface, CollectdShutdownInterface {

	private Logger logger = LoggerFactory.getLogger(Collectd2KafkaPlugin.class);
	private final String __DEFAULT_HOST_PORT = "localhost:2181";
	private String __DEAFULT_TOPIC = "collectd";
	
	private String zk_host_port = __DEFAULT_HOST_PORT;
	private String topic = __DEAFULT_TOPIC;
	private Producer<String, String> kafkaProducer = null;
	
	public Collectd2KafkaPlugin() {
		Collectd.registerConfig(Collectd2KafkaPlugin.class.getSimpleName(), this);
		Collectd.registerInit(Collectd2KafkaPlugin.class.getSimpleName(), this);
		Collectd.registerRead(Collectd2KafkaPlugin.class.getSimpleName(), this);
		Collectd.registerWrite(Collectd2KafkaPlugin.class.getSimpleName(), this);
		Collectd.registerShutdown(Collectd2KafkaPlugin.class.getSimpleName(), this);
	}

	public int config(OConfigItem ci) {
		logger.info("Collectd2KafkaPlugin::config lifecycle", this);
		Collectd.logInfo("Collectd2KafkaPlugin::config lifecycle");
		
		for (OConfigItem item : ci.getChildren()) {
			String key = item.getKey();
			if (key.equalsIgnoreCase("zk.connect")) {
				String value = item.getValues().get(0).getString();
				if (null != value && value.trim() != "") {
					zk_host_port = item.getValues().get(0).getString();
					System.out.println("zk.connect:" + zk_host_port);
				} else {
					logger.info("hey using default value for zk.connect: %s",
							zk_host_port);
					Collectd.logDebug(String.format(
							"hey using default value for zk.connect: %s",
							zk_host_port));

				}
			}

			if (key.equalsIgnoreCase("producer.topic")) {
				String value = item.getValues().get(0).getString();
				if (null != value && value.trim() != "") {
					topic = item.getValues().get(0).getString();
					System.out.println("producer.topic:" + topic);
				} else {
					logger.info(
							"hey using default value for producer.topic: %s",
							topic);
					Collectd.logDebug(String.format(
							"hey using default value for producer.topic: %s",
							topic));

				}
			}
		}
		return 0;
	}

	public int init() {
		logger.info("Collectd2KafkaPlugin::init lifecycle", this);
		Collectd.logInfo("Collectd2KafkaPlugin::init lifecycle");
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
		KeyedMessage<String, String> payload = new KeyedMessage<String, String>(topic, jsonPayload);
		
		if (null != getKafkaProducer()){
			getKafkaProducer().send(payload);
		}
		else{
			logger.warn("Could not obtain kafka Producer discarding payload: %s for topic: %s" + payload.message(), payload.topic());
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

	private Producer<String, String> getKafkaProducer() {
		try {
			if (null == kafkaProducer) {
				Properties props = new Properties();
				props.put("zk.connect", zk_host_port);
				props.put("serializer.class", StringEncoder.class.getName()); //"kafka.serializer.StringEncoder"
				ProducerConfig config = new ProducerConfig(props);
				kafkaProducer = new Producer<String, String>(config);
			}
			
		}catch(Exception e) {
			logger.warn("Error getting kafkaProducer", e);
		}
		return kafkaProducer;
	}

}