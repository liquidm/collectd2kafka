package org.collectd.collectd2kafka;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.collectd.api.Collectd;
import org.collectd.api.CollectdConfigInterface;
import org.collectd.api.CollectdInitInterface;
import org.collectd.api.CollectdWriteInterface;
import org.collectd.api.DataSource;
import org.collectd.api.OConfigItem;
import org.collectd.api.ValueList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Collectd2Kafka implements CollectdWriteInterface, CollectdInitInterface, CollectdConfigInterface {

	private static String zk_host_port = "localhost:2181";
	private static String topic = "collectd";
	private Producer<String, String> kafkaProducer = null;

	private Logger logger = LoggerFactory.getLogger(Collectd2Kafka.class);

	public Collectd2Kafka() {
		Collectd.registerInit(Collectd2Kafka.class.getSimpleName(), this);
		Collectd.registerWrite(Collectd2Kafka.class.getSimpleName(), this);
		Collectd.registerConfig(Collectd2Kafka.class.getSimpleName(), this);
	}

	public int init() {
		Properties props = new Properties();
		props.put("zk.connect", zk_host_port);
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		ProducerConfig config = new ProducerConfig(props);
		kafkaProducer = new Producer<String, String>(config);
		return 0;
	}

	public int write(ValueList vl) {

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
		kafkaProducer.send(payload);

		return 0;

	}

	public static String join(Collection<String> s, String delimiter) {
		StringBuffer buffer = new StringBuffer();
		Iterator<String> iter = s.iterator();
		while (iter.hasNext()) {
			buffer.append(iter.next());
			if (iter.hasNext()) {
				buffer.append(delimiter);
			}
		}
		return buffer.toString();
	}

	public int config(OConfigItem ci) {

		
		System.out.println("registering config...");
		
		boolean usingDefaults = true;
		
		for (OConfigItem item : ci.getChildren()) {
			String key = item.getKey();
			if (key.equalsIgnoreCase("zk.connect")) {
				String value = item.getValues().get(0).getString();
				if (null != value && value.trim() != "") {
					zk_host_port = item.getValues().get(0).getString();
					System.out.println("zk.connect:" + zk_host_port);
					
					
					usingDefaults = false;
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
					usingDefaults = false;
				} else {
					logger.info(
							"hey using default value for producer.topic: %s",
							topic);
					Collectd.logDebug(String.format(
							"hey using default value for producer.topic: %s",
							topic));

				}
			}
			
			if (!usingDefaults) {
				init();
			}
			
			

		}
		return 0;
	}
}