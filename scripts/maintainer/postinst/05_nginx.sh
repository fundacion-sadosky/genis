if [ $FISRT_INSTALL = true ] ; then

  echo "nginx config"

  cat <<EOF > $GENIS_HOME/nginx.conf
user www-data;
worker_processes 4;
pid /run/nginx.pid;

events {
  worker_connections 768;
  # multi_accept on;
}

http {

  ##
  # Basic Settings
  ##

  sendfile on;
  tcp_nopush on;
  tcp_nodelay on;
  keepalive_timeout 65;
  types_hash_max_size 2048;
  # server_tokens off;

  # server_names_hash_bucket_size 64;
  # server_name_in_redirect off;

  include /etc/nginx/mime.types;
  default_type application/octet-stream;

  #proxy_buffering    off;
  #proxy_set_header   X-Real-IP $remote_addr;
  #proxy_set_header   X-Forwarded-Proto $scheme;
  #proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
  #proxy_set_header   Host $http_host;
  #proxy_http_version 1.1;

  ##
  # SSL Settings
  ##

  ssl_protocols TLSv1 TLSv1.1 TLSv1.2; # Dropping SSLv3, ref: POODLE
  ssl_prefer_server_ciphers on;

  ##
  # Logging Settings
  ##

  access_log /var/log/nginx/access.log;
  error_log /var/log/nginx/error.log debug;

  ##
  # Gzip Settings
  ##

  gzip on;
  gzip_disable "msie6";

  # gzip_vary on;
  # gzip_proxied any;
  # gzip_comp_level 6;
  # gzip_buffers 16 8k;
  # gzip_http_version 1.1;
  # gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

  ##
  # Virtual Host Configs
  ##

  include /etc/nginx/conf.d/*.conf;
  #include /etc/nginx/sites-enabled/*;


  server {
    listen         443 ssl;
    server_name    $GENIS_WEB_SUBDOMAIN.$GENIS_ROOT_DOMAIN;

    ssl_certificate     $GENIS_WEB_CERT_PATH;
    ssl_certificate_key $GENIS_WEB_CERTKEY_PATH;

    ssl_client_certificate $GENIS_CA_CERT_PATH;
    ssl_verify_client on;
    ssl_verify_depth 1;

    location / {
      proxy_pass http://localhost:9000;
    }

  }

  server {
    listen         443 ssl;
    server_name    $GENIS_I2C_SUBDOMAIN.$GENIS_ROOT_DOMAIN;

    ssl_certificate     $GENIS_I2C_CERT_PATH;
    ssl_certificate_key $GENIS_I2C_CERTKEY_PATH;

    ssl_client_certificate $GENIS_CA_CERT_PATH;
    ssl_verify_client on;
    ssl_verify_depth 1;

    location / {
      proxy_pass http://localhost:9000;
    }

  }

}
EOF

  ln --backup -s $GENIS_HOME/nginx.conf /etc/nginx/nginx.conf
  service nginx restart || true
  
fi  