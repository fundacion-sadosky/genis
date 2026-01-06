#!/usr/bin/env bash

cd /home/pdg
rm -rf certificates
mkdir certificates
cd certificates

# Client CA

	# Genero clave privada
openssl genrsa -des3 -passout pass:certauth -out genisCA.key 4096

	# Genero el certificado de la CA
openssl req -new -x509 -days 365 -key genisCA.key -out genisCA.crt -passin pass:certauth  -subj "/C=AR/ST=BA/L=CABA/O=Fundacion Sadosky/CN=genisCA"

# Cliente
	# Genero clave privada
openssl genrsa -out pdg-devclient.key 2048

	# Genero el sign request
openssl req -new -key pdg-devclient.key -out pdg-devclient.csr -subj "/C=AR/ST=BA/L=CABA/O=Fundacion Sadosky/CN=pdg-devclient"

# Client CA
	# El CA firma el sign request del cliente
openssl x509 -req -days 365 -in pdg-devclient.csr -CA genisCA.crt -CAkey genisCA.key -set_serial 001 -out pdg-devclient.crt -passin pass:certauth

# Server
	# Genero clave privada
openssl genrsa -out example.com.key

	# Genero sign request
openssl req -new -key example.com.key -out example.com.csr -subj "/C=AR/ST=BA/L=CABA/O=Fundacion Sadosky/CN=example.com"

	#  El CA firma el sign request del cliente
openssl x509 -req -days 365 -in example.com.csr -CA genisCA.crt -CAkey genisCA.key -set_serial 002 -out example.com.crt  -passin pass:certauth

# Cliente

    # Genero Truststore para que el cliente confie en el certificado del servidor
keytool -import -v -trustcacerts -alias instsup -file example.com.crt -keystore clientTrustore.jks -keypass cliente12345 -storepass cliente12345  << EOF
yes
EOF

    # Genero Keystore del cliente en 2 pasos

    # Paso 1 creo el .pfx
openssl pkcs12 -export -out pdg-devclient.pfx -inkey pdg-devclient.key -in pdg-devclient.crt -passout pass:cliente123456

    # Paso 2 importo el .pfx en el .jks

keytool -importkeystore -srckeystore pdg-devclient.pfx -srcstoretype pkcs12 -destkeystore clientKeystore.jks -deststoretype JKS -deststorepass cliente123456 -srcstorepass cliente123456
