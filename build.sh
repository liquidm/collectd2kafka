
CWD=$(pwd)

mvn deploy:deploy-file \
  -Durl="file://$CWD/repo/"  \
  -Dfile=libs/kafka_2.8.0-0.8.0.jar \
  -DgroupId=org.apache.kafka \
  -DartifactId=kafka_2.8.0 \
  -Dpackaging=jar \
  -Dversion=0.8.0-LIQUIDM

mvn deploy:deploy-file \
  -Durl="file://$CWD/repo/" \
  -Dfile=libs/collectd-api.jar \
  -DgroupId=org.collectd2kafka \
  -DartifactId=collectd-api \
  -Dpackaging=jar \
  -Dversion=0.0.1-LIQUIDM
 

mvn clean package
