import collectd
import random
import json 
from kafka.client import KafkaClient
from kafka.producer import SimpleProducer

brokers = "darkstar.lqm.io:9092"
topic = "collectd"

def config_callback(conf):
    global brokers, topic
    for node in conf.children:
        if node.key == 'Brokers':
            brokers = node.values[0]
        elif node.key == 'Topic':
            topic = node.values[0]

collectd.register_config(config_callback);

def write_callback(v, data=None):
    metric = {}
    metric['host'] = v.host
    metric['plugin'] = v.plugin
    metric['plugin_instance'] = v.plugin_instance
    metric['type'] = v.type
    metric['type_instance'] = v.type_instance
    metric['time'] = v.time
    metric['interval'] = v.interval
    metric['values'] = []

    for value in v.values:
        metric['values'].append(value)

    producer.send_messages("collectd", json.dumps(metric))

kafka = KafkaClient(brokers)
producer = SimpleProducer(kafka)
collectd.register_write(write_callback);
