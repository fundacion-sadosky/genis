if [ $FISRT_INSTALL = true ] ; then

	echo "postgres config"

	if ! id -u $GENIS_PG_USR > /dev/null 2>&1; then
		adduser --disabled-password --no-create-home --gecos "" $GENIS_PG_USR
	else			
		echo "$GENIS_PG_USR already exists in OS"
	fi	
	
	if ! sudo -u $PG_SRV_USER psql $PG_SRV_USER -tAc "SELECT 1 FROM pg_roles WHERE rolname='$GENIS_PG_USR'" | grep -q 1; then
		sudo -u $PG_SRV_USER psql -c "CREATE USER \"$GENIS_PG_USR\" WITH NOSUPERUSER NOCREATEROLE CREATEDB UNENCRYPTED PASSWORD '$GENIS_PG_PWD';"
	else
		echo "$GENIS_PG_USR already exists in Postgre"
	fi

	if ! sudo -u $GENIS_PG_USR psql -lqt | cut -d \| -f 1 | grep -qw genisdb; then
		sudo -u $GENIS_PG_USR createdb genisdb
	else
		echo "genisdb already exists"
	fi

	if ! sudo -u $GENIS_PG_USR psql -lqt | cut -d \| -f 1 | grep -qw genislogdb; then
		sudo -u $GENIS_PG_USR createdb genislogdb
	else
		echo "genislogdb already exists"
	fi
fi
