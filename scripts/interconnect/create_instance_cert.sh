#!/usr/bin/env bash

read -p "Please enter genis instance doamin:" input
INSTANCE_DOMAIN=${input}

CA_FOLDER=`pwd`
read -p "Please enter CA cert and key folder ${CA_FOLDER:+[$CA_FOLDER]}:" input
CA_FOLDER=${input:-$CA_FOLDER}

OUTPUT_FOLDER=`pwd`
read -p "Please enter destintion folder ${OUTPUT_FOLDER:+[$OUTPUT_FOLDER]}:" input
OUTPUT_FOLDER=${input:-$OUTPUT_FOLDER}


# Genero clave privada para la instancia
openssl genrsa \
	-out "$OUTPUT_FOLDER/$INSTANCE_DOMAIN.key" \
	4096

# Genero sign request 
openssl req \
	-new \
	-key "$OUTPUT_FOLDER/$INSTANCE_DOMAIN.key" \
	-subj "/C=AR/ST=BA/L=CABA/O=Fundacion Sadosky/CN=$INSTANCE_DOMAIN" \
	-out "$OUTPUT_FOLDER/$INSTANCE_DOMAIN.csr" 

#  El CA firma el sign request de la instancia
openssl x509 \
	-req \
	-days 365 \
	-set_serial 002 \
	-passin pass:certauth \
	-CA "$CA_FOLDER/genis_ca.crt" \
	-CAkey "$CA_FOLDER/genis_ca.key" \
	-in "$OUTPUT_FOLDER/$INSTANCE_DOMAIN.csr" \
	-out "$OUTPUT_FOLDER/$INSTANCE_DOMAIN.crt" 

# Genero Keystore del cliente en 2 pasos
# Paso 1 creo el .pfx
openssl pkcs12 \
	-export \
	-passout pass:cliente123456 \
	-in "$OUTPUT_FOLDER/$INSTANCE_DOMAIN.crt" \
	-inkey "$OUTPUT_FOLDER/$INSTANCE_DOMAIN.key" \
	-out "$OUTPUT_FOLDER/$INSTANCE_DOMAIN.pfx" 
# Paso 2 importo el .pfx en el .jks
keytool \
	-importkeystore \
	-srcstoretype pkcs12 \
	-srckeystore "$OUTPUT_FOLDER/$INSTANCE_DOMAIN.pfx" \
	-srcstorepass cliente123456 \
	-destkeystore "$OUTPUT_FOLDER/${INSTANCE_DOMAIN}_keystore.jks" \
	-deststoretype JKS \
	-deststorepass cliente123456 

# Cliente
# Genero Truststore para que el cliente confie en la CA de genis
keytool \
	-v \
	-import \
	-trustcacerts \
	-alias instsup \
	-file "$CA_FOLDER/genis_ca.crt" \
	-keystore "$OUTPUT_FOLDER/${INSTANCE_DOMAIN}_truststore.jks" \
	-keypass cliente12345 \
	-storepass cliente12345  <<EOF
yes
EOF

# Remuevo archivos temporales
rm "$OUTPUT_FOLDER/${INSTANCE_DOMAIN}.csr"
rm "$OUTPUT_FOLDER/${INSTANCE_DOMAIN}.pfx"

echo "
###############################################################################
###############################################################################
###############################################################################

Archivos necesarios para configurar NGINX para recibir conexiones HTTPS con
autenticacvion mutua:

	$OUTPUT_FOLDER/${INSTANCE_DOMAIN}.crt
	$OUTPUT_FOLDER/${INSTANCE_DOMAIN}.key
	$CA_FOLDER/genis_ca.key

Archivos necesarios para configurar cliente Play WS para iniciar conexiones 
HTTPS con autenticacvion mutua:

	$OUTPUT_FOLDER/${INSTANCE_DOMAIN}_keystore.jks
	$OUTPUT_FOLDER/${INSTANCE_DOMAIN}_truststore.jks
	
###############################################################################
###############################################################################
###############################################################################
"