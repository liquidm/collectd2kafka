import collectd
import random
from json import JSONEncoder as json_enc
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

def read_callback(data=None):
    vl = collectd.Values(type='gauge')
    vl.plugin = 'collecd2kafka'
    vl.dispatch(values=[random.random() * 100])

def write_callback(vl, data=None):
    for x in vl.values:
        jdata = json_enc().encode({vl.plugin : [str(vl.type), str(x)]})
        producer.send_messages("collectd", jdata)

collectd.register_config(config_callback);
kafka = KafkaClient(brokers)
producer = SimpleProducer(kafka)
collectd.register_read(read_callback);
collectd.register_write(write_callback);
