package org.collectd.collectd2kafka;

import static org.junit.Assert.assertNotNull;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Collectd2KafkaPluginTest {
 
    static Collectd2KafkaPlugin plugin = null;
    
    @BeforeClass
    public static void testSetup() {
        plugin = new Collectd2KafkaPlugin();
    }

    @Test
    public void producerConfigDefault() {
        ProducerConfig config = plugin.getProducerConfig(Collectd2KafkaPlugin.kafkaProducerDefaultProperties());
        assertNotNull(config);
    }

    @Test
    public void producerIsNotNull() {
        Producer<String, String> producer = plugin.getKafkaProducer();
        assertNotNull(producer);
    }

}