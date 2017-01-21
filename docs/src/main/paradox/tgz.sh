curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.14.0.tgz
tar xvfz akka-wamp-0.14.0.tar.gz
cd akka-wamp-0.14.0
vim ./conf/application.conf
./bin/akka-wamp -Dakka.loglevel=DEBUG