#!/usr/bin/env bash

cd /home/pdg
rm -rf certificates-uat
mkdir certificates-uat
cd certificates-uat

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
openssl genrsa -out pdg-uat.key

	# Genero sign request
openssl req -new -key pdg-uat.key -out pdg-uat.csr -subj "/C=AR/ST=BA/L=CABA/O=Fundacion Sadosky/CN=pdg-uat"

	#  El CA firma el sign request del cliente
openssl x509 -req -days 365 -in pdg-uat.csr -CA genisCA.crt -CAkey genisCA.key -set_serial 002 -out pdg-uat.crt  -passin pass:certauth

# Cliente

    # Genero Truststore para que el cliente confie en el certificado del servidor
keytool -import -v -trustcacerts -alias instsup -file pdg-uat.crt -keystore clientTrustore.jks -keypass cliente12345 -storepass cliente12345  << EOF
yes
EOF

    # Genero Keystore del cliente en 2 pasos

    # Paso 1 creo el .pfx
openssl pkcs12 -export -out pdg-devclient.pfx -inkey pdg-devclient.key -in pdg-devclient.crt -passout pass:cliente123456

    # Paso 2 importo el .pfx en el .jks

keytool -importkeystore -srckeystore pdg-devclient.pfx -srcstoretype pkcs12 -destkeystore clientKeystore.jks -deststoretype JKS -deststorepass cliente123456 -srcstorepass cliente123456


# Cliente Instancia Superior

    # Genero Truststore para que el cliente confie en el certificado del servidor
keytool -import -v -trustcacerts -alias instsup -file pdg-devclient.crt -keystore clientTrustoreUAT.jks -keypass cliente12345 -storepass cliente12345  << EOF
yes
EOF

    # Genero Keystore del cliente en 2 pasos

    # Paso 1 creo el .pfx
openssl pkcs12 -export -out pdg-uat.pfx -inkey pdg-uat.key -in pdg-uat.crt -passout pass:cliente123456

    # Paso 2 importo el .pfx en el .jks

keytool -importkeystore -srckeystore pdg-uat.pfx -srcstoretype pkcs12 -destkeystore clientKeystoreUAT.jks -deststoretype JKS -deststorepass cliente123456 -srcstorepass cliente123456







# Cliente mara
	# Genero clave privada
openssl genrsa -out pdg-mara.key 2048

	# Genero el sign request
openssl req -new -key pdg-mara.key -out pdg-mara.csr -subj "/C=AR/ST=BA/L=CABA/O=Fundacion Sadosky/CN=pdg-mara"

# Client CA
	# El CA firma el sign request del cliente
openssl x509 -req -days 365 -in pdg-mara.csr -CA genisCA.crt -CAkey genisCA.key -set_serial 001 -out pdg-mara.crt -passin pass:certauth


    # Genero Keystore del cliente en 2 pasos

    # Paso 1 creo el .pfx
openssl pkcs12 -export -out pdg-mara.pfx -inkey pdg-mara.key -in pdg-mara.crt -passout pass:cliente123456

    # Paso 2 importo el .pfx en el .jks

keytool -importkeystore -srckeystore pdg-mara.pfx -srcstoretype pkcs12 -destkeystore clientKeystoreMara.jks -deststoretype JKS -deststorepass cliente123456 -srcstorepass cliente123456



