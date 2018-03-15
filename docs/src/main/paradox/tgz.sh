curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.15.2.tgz
tar xvfz akka-wamp-0.15.2.tar.gz
cd akka-wamp-0.15.2
vim ./conf/application.conf
./bin/akka-wamp -Dakka.loglevel=DEBUG
