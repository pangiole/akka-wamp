#!/usr/bin/env bash

SRC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
# #make-example-cert
PASSWD=changeit
ROUTER_HOME=/path/to/router/home

# It creates/updates a key store with
#
#    - a new public/private key pair, and
#    - a new certificate owned by example.com
#
# #make-example-cert

ROUTER_HOME=$SRC
rm $ROUTER_HOME/example.com.* $ROUTER_HOME/root-ca.*

# #make-example-cert
keytool -genkeypair -v \
  -alias example.com \
  -dname "CN=example.com, OU=Example Org, O=Example Company, L=London, ST=London, C=UK" \
  -keystore $ROUTER_HOME/example.com.jks \
  -keypass $PASSWD \
  -storepass $PASSWD \
  -keyalg RSA \
  -keysize 2048 \
  -validity 365
# #make-example-cert



# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# #make-ca-cert
# For test purposes, it creates/updates a "fake" root CA
# key store with:
#
#    - a new public/private key pair, and
#    - a new self-signed root CA certificate
#
# This fake root CA will issue/sign the example.com certificate
#
keytool -genkeypair -v \
  -alias root-ca \
  -dname "CN=Root Authority, OU=Root Org, O=Root Company, L=San Francisco, ST=California, C=US" \
  -keystore $ROUTER_HOME/root-ca.jks \
  -keypass $PASSWD \
  -storepass $PASSWD \
  -keyalg RSA \
  -keysize 4096 \
  -ext KeyUsage:critical="keyCertSign" \
  -ext BasicConstraints:critical="ca:true" \
  -validity 9999

# Exports the root CA certificate from the above key store to the
# root-ca.crt file, so that it can easily be imported into client's
# trust stores
#
keytool -export -v \
  -alias root-ca \
  -file $ROUTER_HOME/root-ca.crt \
  -keypass $PASSWD \
  -storepass $PASSWD \
  -keystore $ROUTER_HOME/root-ca.jks \
  -rfc
# #make-ca-cert


# Imports the root CA certificate into the example.com trust store
#
keytool -import -v \
  -alias root-ca \
  -file $ROUTER_HOME/root-ca.crt \
  -keystore $ROUTER_HOME/example.com.jks \
  -storetype JKS \
  -storepass $PASSWD<< EOF
yes
EOF



# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# #csr
# Creates a CSR - Certificate Signing Request for the
# example.com certificate
#
keytool -certreq -v \
  -alias example.com \
  -keypass $PASSWD \
  -storepass $PASSWD \
  -keystore $ROUTER_HOME/example.com.jks \
  -file $ROUTER_HOME/example.com.csr

# Simulates CSR submission and completion of the
# example.com certificate
#
keytool -gencert -v \
  -alias root-ca \
  -keypass $PASSWD \
  -storepass $PASSWD \
  -keystore $ROUTER_HOME/root-ca.jks \
  -infile $ROUTER_HOME/example.com.csr \
  -outfile $ROUTER_HOME/example.com.crt \
  -ext KeyUsage:critical="digitalSignature,keyEncipherment" \
  -ext EKU="serverAuth" \
  -ext SAN="DNS:example.com" \
  -rfc

# Finally, imports the signed certificate back into
# the example.com key store
#
keytool -import -v \
  -alias example.com \
  -file $ROUTER_HOME/example.com.crt \
  -keystore $ROUTER_HOME/example.com.jks \
  -storetype JKS \
  -storepass $PASSWD
# #csr


# List out the contents of $ROUTER_HOME/example.com.jks just to confirm it.  
# If you are using Play as a TLS termination point,
# this is the key store you should present as the server.
keytool -list -v \
  -keystore $ROUTER_HOME/example.com.jks \
  -storepass $PASSWD

echo "Password is $PASSWD"
