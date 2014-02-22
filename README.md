collectd2kafka
==============

Description
================================

  collectd2kafka is a collectd plugin implementation with the goal to stream metrics over a kafka topic using a message producer.

Build Requirements
================================

  * collectd: https://collectd.org

  * Apache Maven: http://maven.apache.org

  * Apache Kafka 0.8.0 http://kafka.apache.org

Usage
================================

  * mvn clean install

  * mvn clean install -DskipTests=false (run build with integration tests included)

  * sh build.sh (refreshes the non managed maven libraries from libs/* into local repo as defined on build script)

Plugin Configuration Properties (kafka Producer)
================================

http://kafka.apache.org/08/configuration.html

 * metadata.broker.list, e.g. metadata.broker.list=localhost:9092

 * producer.topic, e.g. producer.topic=collectd


