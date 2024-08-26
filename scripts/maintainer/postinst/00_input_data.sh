echo "Genis config..."

[ -f "/etc/default/$app_name" ] && FISRT_INSTALL=false || FISRT_INSTALL=true

if [ $FISRT_INSTALL = true ] ; then
	PG_SRV_USER=postgres
	LDAP_SRV_USER=openldap
	MONGO_SRV_USER=root
	NGINX_SRV_USER=www-data
	PLAY_SRV_USR=$app_name

	GENIS_ENV_FILE=/etc/default/$app_name    

	GENIS_VERSION=4.0.0
	read -p "Please enter genis home directory path [/etc/genis]: " input
	GENIS_HOME=${input:-"/etc/genis"}
	GENIS_CONF_FILE=$GENIS_HOME/genis-application.conf
	GENIS_LOG_CONF_FILE=$GENIS_HOME/logger.xml

	read -p "Please enter Openldap admin user password: " input
	LDAP_ADMIN_PWD=${input}

	read -p "Please enter default instance user name [genis-admin]: " input
	GENIS_DEFAULT_USR_CN=${input:-"genis-admin"}
	read -p "Please enter default instance user password: " input
	GENIS_DEFAULT_USR_PWD=$input
	GENIS_DEFAULT_USR_PWD_SHA=`slappasswd -h {SSHA} -s $GENIS_DEFAULT_USR_PWD`
	#otp -D16 -e 
	# openssl enc -e -aes-256-cbc -in un_encrypted.data -out encrypted.data -k $LDAP_ADMIN_PWD -S agentsalt -iter 20000
	# openssl enc -e -aes-256-cbc -in un_encrypted.data -out encrypted.data -K key -iv IV -S agentsalt 
	GENIS_DEFAULT_USR_ENCRYPTED_TOTP="/2kxUT3GHauLAyI0RwL5M5lerSsmyu01EDPs6S+mO5s="

	read -p "Please enter mongodb conection user name [genis-mongo-usr]: " input
	GENIS_MONGO_USR=${input:-"genis-mongo-usr"}
	read -p "Please enter mongodb conection user password: " input
	GENIS_MONGO_PWD=$input


	read -p "Please enter PostgreSQL conection user name [genis-pg-usr]: " input
	GENIS_PG_USR=${input:-"genis-pg-usr"}
	read -p "Please enter PostgreSQL conection user password: " input
	GENIS_PG_PWD=$input

	read -p "Please enter instance country code[AR]: " input
	GENIS_COUNTRY_CODE=${input:-"AR"}
	read -p "Please enter instance province code: " input
	GENIS_PROVINCE_CODE=$input
	read -p "Please enter instance laboratory code: " input
	GENIS_LABORATORY_CODE=$input

	read -p "Please enter root domain name [genis.local]:" input
	GENIS_ROOT_DOMAIN=${input:-"genis.local"}
	GENIS_LDAP_DC="dc="`echo $GENIS_ROOT_DOMAIN | sed "s/\./,dc=/g"`

	read -p "Please enter web client subdomain name [genis]: " input
	GENIS_WEB_SUBDOMAIN=${input:-"genis"}
	read -p "Please enter web client certificate path [$GENIS_HOME/$GENIS_WEB_SUBDOMAIN.$GENIS_ROOT_DOMAIN.crt]: " input
	GENIS_WEB_CERT_PATH=${input:-"$GENIS_HOME/$GENIS_WEB_SUBDOMAIN.$GENIS_ROOT_DOMAIN.crt"}
	read -p "Please enter web client certificate key path [$GENIS_HOME/$GENIS_WEB_SUBDOMAIN.$GENIS_ROOT_DOMAIN.key]: " input
	GENIS_WEB_CERTKEY_PATH=${input:-"$GENIS_HOME/$GENIS_WEB_SUBDOMAIN.$GENIS_ROOT_DOMAIN.key"}

	read -p "Please enter interconnection subdomain name [i2c]: " input
	GENIS_I2C_SUBDOMAIN=${input:-"i2c"}
	read -p "Please enter interconnection certificate path [$GENIS_HOME/$GENIS_I2C_SUBDOMAIN.$GENIS_ROOT_DOMAIN.crt]: " input
	GENIS_I2C_CERT_PATH=${input:-"$GENIS_HOME/$GENIS_I2C_SUBDOMAIN.$GENIS_ROOT_DOMAIN.crt"}
	read -p "Please enter interconnection certificate key path [$GENIS_HOME/$GENIS_I2C_SUBDOMAIN.$GENIS_ROOT_DOMAIN.key]: " input
	GENIS_I2C_CERTKEY_PATH=${input:-"$GENIS_HOME/$GENIS_I2C_SUBDOMAIN.$GENIS_ROOT_DOMAIN.key"}

	read -p "Please enter genis CA cert path [$GENIS_HOME/CA.crt]: " input
	GENIS_CA_CERT_PATH=${input:-"$GENIS_HOME/CA.crt"}
	read -p "Please enter interconnection client keystore path [$GENIS_HOME/i2c_client.jks]: " input
	GENIS_CLIENT_JKS_PATH=${input:-"$GENIS_HOME/i2c_client.jks"}
	read -p "Please enter interconnection client keystore password: " input
	GENIS_CLIENT_JKS_PWD=$input
	read -p "Please enter interconecction trust store path [$GENIS_HOME/i2c_trust.jks]: " input
	GENIS_TRUST_JKS_PATH=${input:-"$GENIS_HOME/i2c_trust.jks"}
else
	echo "Already instaled skip asking config data ..."
fi
