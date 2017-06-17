curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.15.1.tgz
tar xvfz akka-wamp-0.15.1.tar.gz
cd akka-wamp-0.15.1
vim ./conf/application.conf
./bin/akka-wamp -Dakka.loglevel=DEBUG
