package org.collectd.collectd2kafka;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.serializer.StringEncoder;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
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
        ProducerConfig config = plugin.getProducerConfig(Collectd2KafkaPlugin.kafkaProducerDefaultProperties());
        assertNotNull(config);
    }

    @Test
    public void producerIsNotNull() {
        Producer<String, String> producer = plugin.getKafkaProducer();
        assertNotNull(producer);
    }
    
    
    /**
     * IT requires a running kafka server
     * 
     */
    @Test
    @Ignore
    public void kafkaProducerSendMessage() {
        
        Map<String, String> test = new HashMap<String, String>();
        test.put("test", "Hello");
        JsonNode json = new ObjectMapper().valueToTree(test);
        String jsonPayload = json.toString();
        KeyedMessage<String, String> payload = new KeyedMessage<String, String>("collectd-test", jsonPayload);
        Producer<String, String> producer = plugin.getKafkaProducer();
  
        producer.send(payload);
    }
    
    
    /**
     * IT requires a running kafka server
     * 
     */
    @Test
    @Ignore
    public void kafkaProducerSendMessage2() {
        // see http://kafka.apache.org/08/configuration.html
        /*
            metadata.broker.list    => no default (required)
            request.required.acks   => default to 0
            producer.type   => default to sync
            serializer.class => default to kafka.serializer.DefaultEncoder The serializer class for messages. The default encoder takes a byte[] and returns the same byte[].
            client.id => defaults to ""  The client id is a user-specified string sent in each request to help trace calls. 
                         It should logically identify the application making the request.

         */

        
        Map<String, String> test = new HashMap<String, String>();
        test.put("test", "Hello");
        JsonNode json = new ObjectMapper().valueToTree(test);
        String jsonPayload = json.toString();
        KeyedMessage<String, String> payload = new KeyedMessage<String, String>("collectd-test", jsonPayload);
        
        
        Properties props = new Properties();
        props.put("client.id", Collectd2KafkaPlugin.class.getName()); //TODO: add host information addition in order to identify host as well
        props.put("serializer.class", StringEncoder.class.getName()); // "kafka.serializer.StringEncoder"
        props.put("metadata.broker.list", "localhost:9092");
        
        Producer<String, String> producer = plugin.getKafkaProducer(props);

        producer.send(payload);
    }
    
    
}