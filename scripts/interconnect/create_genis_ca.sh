#!/usr/bin/env bash

OUTPUT_FOLDER=`pwd`
read -p "Please enter destintion folder ${OUTPUT_FOLDER:+[$OUTPUT_FOLDER]}:" input
OUTPUT_FOLDER=${input:-$OUTPUT_FOLDER}

# Genero clave privada
openssl genrsa \
	-des3 \
	-passout pass:certauth \
	-out "$OUTPUT_FOLDER/genis_ca.key" \
	4096

# Genero el certificado de la CA
openssl req \
	-new \
	-x509 \
	-days 365 \
	-subj "/C=AR/ST=BA/L=CABA/O=Fundacion Sadosky/CN=genisCA" \
	-passin pass:certauth \
	-key $OUTPUT_FOLDER/genis_ca.key \
	-out $OUTPUT_FOLDER/genis_ca.crt 

