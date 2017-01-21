#!/usr/bin/env bash

BIN="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# #make-trust-store
PASSWD=changeit
ROUTER_HOME=/path/to/router/home
CLIENT_HOME=/path/to/client/home

# #make-trust-store

ROUTER_HOME=$BIN/../router
CLIENT_HOME=$BIN
rm -f $CLIENT_HOME/trust-store.jks

# #make-trust-store
# It creates/updates a trust store with
#
#    - the certificate of the fake root CA who issue/signed
#      the example.com certificate
#
keytool -import -v \
  -alias root-ca \
  -file $ROUTER_HOME/root-ca.crt \
  -keypass $PASSWD \
  -storepass $PASSWD \
  -keystore $CLIENT_HOME/trust-store.jks << EOF
yes
EOF
# #make-trust-store


# List out the details of the store.
keytool -list -v \
  -keystore $CLIENT_HOME/trust-store.jks \
  -storepass $PASSWD

echo "Password is $PASSWD"



