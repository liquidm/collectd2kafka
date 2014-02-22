package org.collectd.collectd2kafka;

import static org.junit.Assert.*;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;

import org.junit.BeforeClass;
import org.junit.Ignore;
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

    /*
     * should fail where jni can not load native code
     */
    @Test
    @Ignore
    public void init() {
       plugin.init();
    }


    @Test
    public void producerConfigDefault() {
        ProducerConfig config = plugin.buildDefaultProducerConfig();
        assertNotNull(config);
    }

    @Test
    public void producerIsNotNull() {
        Producer<String, String> producer = plugin.getKafkaProducer();
        assertNotNull(producer);
    }
    
    
}