import ConfigParser as conf_p
import os, collectd, random, json
from kafka.client import KafkaClient
from kafka.producer import SimpleProducer

COLLECTD_PYTHON_CONF = '/etc/collectd/collectd2kafka.py.conf'

def parse_types_file(path):
    f = open(path, 'r')

    types = {}
    for line in f:
        fields = line.split()
        if len(fields) < 2:
            continue

        type_name = fields[0]

        if type_name[0] == '#':
            continue

        v = []
        for ds in fields[1:]:
            ds = ds.rstrip(',')
            ds_fields = ds.split(':')

            if len(ds_fields) != 4:
                collectd.warning('collectd2python: cannot parse data source %s on type %s' % ( ds, type_name ))
                continue

            v.append(ds_fields)

        types[type_name] = v

    f.close()
    return types

def get_config():
    config = conf_p.ConfigParser()
    config.read(CONFIG_FILE)
    global KAFKA_BROKERS
    global TOPIC
    global TYPES
    KAFKA_BROKERS = []
    TYPES = {}
    for x in config.get('collectd2kafka', 'Brokers')
        KAFKA_BROKERS.append(x)
    TOPIC = config.get('collectd2kafka', 'Topic')
    TYPES.update(parse_types_file(config.get('collectd2kafka', 'TypesDB')))

def config_callback(conf):
    global KAFKA_BROKERS
    global TOPIC
    global TYPES
    KAFKA_BROKERS = []
    TYPES = {}


    for node in conf.children:
        if node.key == 'Brokers':
            for x in node.values:
                print x
                KAFKA_BROKERS.append(x)
        elif node.key == 'Topic':
            TOPIC = node.values[0]
        elif node.key == 'TypesDB':
            for x in node.values:
                TYPES.update(parse_types_file(x)) 


def write_callback(v, data=None):
    if v.type not in TYPES:
        collectd.warning('collectd2kafka: cannot handle type %s. check types.db file?' % v.type)
        return

    v_type = TYPES[v.type]

    if len(v_type) != len(v.values):
        collectd.warning('collectd2kafka: more values than type %s' % v.type)
        return


    metric = {}
    metric['host'] = v.host
    metric['plugin'] = v.plugin
    metric['plugin_instance'] = v.plugin_instance
    metric['type'] = v.type
    metric['type_instance'] = v.type_instance
    metric['time'] = v.time
    metric['interval'] = v.interval
    metric['values'] = []


    i = 0
    for value in v.values:
        s_name = v_type[i][0]
        ds_type = v_type[i][1]

        metric['values'].append(value)

    producer.send_messages(TOPIC, json.dumps(metric))
print "!!!!!!!!!!!!!!!!!!!"
print str(KAFKA_BROKERS)
print "!!!!!!!!!!!!!!!!!!!"

get_config();
#collectd.register_config(config_callback);
collectd.register_write(write_callback);
kafka = KafkaClient(KAFKA_BROKERS)
producer = SimpleProducer(kafka)
