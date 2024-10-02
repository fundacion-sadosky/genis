
if [ $FISRT_INSTALL = true ] ; then
  echo "Genis config..."

  unlink /etc/genis || true

  if [ -f $GENIS_ENV_FILE ]; then
     echo "GENis environment variable file already exists: $GENIS_ENV_FILE"
     echo "\tWill not override with entered data"
     . $ENV_FILE
  else
    echo "Creating GENis environment variables file: $GENIS_ENV_FILE"
    touch $GENIS_ENV_FILE 
    chown "$app_name:$app_name" $GENIS_ENV_FILE
    cat <<EOF > $GENIS_ENV_FILE
PG_SRV_USER=$PG_SRV_USER
LDAP_SRV_USER=$LDAP_SRV_USER
MONGO_SRV_USER=$MONGO_SRV_USER
NGINX_SRV_USER=$NGINX_SRV_USER
PLAY_SRV_USR=$app_name
GENIS_HOME=$GENIS_HOME
GENIS_VERSION=$GENIS_VERSION
CMD_OPTS="-DapplyEvolutions.default=true -DapplyDownEvolutions.default=true -DapplyEvolutions.logDb=true -DapplyDownEvolutions.logDb=true -Dconfig.file=\$GENIS_HOME/genis-application.conf -Dlogger.file=\$GENIS_HOME/logger.xml"
EOF
  fi


  [ -d $GENIS_HOME ] || install -d -o "$app_name" -m750 $GENIS_HOME

  if [ -f $GENIS_CONF_FILE ]; then
     echo "GENis application conf file already exists: $GENIS_CONF_FILE"
     echo "\tWill not override with entered data"
  else
    echo "Creating GENis application conf file: $GENIS_CONF_FILE"
    touch $GENIS_CONF_FILE 
    chown "$app_name:$app_name" $GENIS_CONF_FILE 
    cat <<EOF > $GENIS_CONF_FILE
include "application.conf"

# LDAP Configuration 
ldap {
  default {
    adminDn = "cn=admin,$GENIS_LDAP_DC"
    adminPassword=$LDAP_ADMIN_PWD 
    usersDn = "ou=Users,$GENIS_LDAP_DC"
    rolesDn = "ou=Roles,$GENIS_LDAP_DC"
  }
}

# Database configuration
db {
  default {
    user = "$GENIS_PG_USR"
    password = "$GENIS_PG_PWD"
  }  
  logDb {
    user = "$GENIS_PG_USR"
    password = "$GENIS_PG_PWD"
  }
}

laboratory {
  country = $GENIS_COUNTRY_CODE
  province = $GENIS_PROVINCE_CODE
  code = $GENIS_LABORATORY_CODE
}

#Superior
defaultAssignee = $GENIS_DEFAULT_USR_CN

ws.ssl {

  keyManager = {
    stores = [
      { 
        type: "JKS", 
        path: "$GENIS_CLIENT_JKS_PATH", 
        password: "$GENIS_CLIENT_JKS_PWD"  
      }
    ]
  }
  trustManager = {
    stores = [
      { 
        type = "JKS", 
        path: "$GENIS_TRUST_JKS_PATH" 
      }
    ]
  }
}
EOF
  fi

  if [ -f $GENIS_LOG_CONF_FILE ]; then
     echo "GENis logger config file already exists: $GENIS_LOG_CONF_FILE"
     echo "\tWill not override with entered data"
  else
    echo "Creating GENis logger conf file: $GENIS_LOG_CONF_FILE"
    touch $GENIS_LOG_CONF_FILE 
    chown "$app_name:$app_name" $GENIS_LOG_CONF_FILE 
    cat <<EOF > $GENIS_LOG_CONF_FILE
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>\${application.home}/logs/application.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>\${application.home}/logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
    </rollingPolicy>
    <encoder>
      <pattern>%date [%level] %logger: %message [%thread]%n%xException</pattern>
    </encoder>
  </appender>

  <logger name="play" level="INFO"/>
  <logger name="akka" level="INFO"/>
  <logger name="audit" level="DEBUG" />
  <logger name="configdata" level="DEBUG" />
  <logger name="bulkupload" level="DEBUG" />
  <logger name="Global" level="DEBUG" />
  <logger name="controllers" level="DEBUG" />
  <logger name="injector" level="DEBUG" />
  <logger name="laboratories" level="DEBUG" />
  <logger name="matching" level="DEBUG"/>
  <logger name="pedigree" level="DEBUG"/>
  <logger name="models" level="DEBUG" />
  <logger name="notifications" level="DEBUG" />
  <logger name="probability" level="DEBUG" />
  <logger name="profile" level="DEBUG" />
  <logger name="profiledata" level="DEBUG" />
  <logger name="search" level="DEBUG" />
  <logger name="security" level="DEBUG" />
  <logger name="services" level="DEBUG" />
  <logger name="stats" level="DEBUG" />
  <logger name="models" level="DEBUG" />
  <logger name="util" level="DEBUG" />
  <logger name="views" level="DEBUG" />
  <logger name="user" level="DEBUG" />
  <logger name="audit.PEOSignerActor" level="INFO" />

  <logger name="slick.jdbc.JdbcBackend.statement" level="OFF" />
  <logger name="org.mockito" level="OFF" />

  <logger name="org.apache.spark" level="ERROR" />
  <logger name="org.mongodb.driver" level="WARN" />

  <!-- Off these ones as they are annoying, and anyway we manage configuration 
    ourself -->
  <logger name="com.avaje.ebean.config.PropertyMapLoader" level="OFF" />
  <logger name="com.avaje.ebeaninternal.server.core.XmlConfigLoader" level="OFF" />
  <logger name="com.avaje.ebeaninternal.server.lib.BackgroundThread" level="OFF" />
  <logger name="com.gargoylesoftware.htmlunit.javascript" level="OFF" />

  <root level="ERROR">
    <appender-ref ref="FILE" />
  </root>

</configuration>
EOF
  fi

  systemctl enable $app_name

fi

