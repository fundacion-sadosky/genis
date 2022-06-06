#! /bin/bash
### BEGIN INIT INFO
# Provides:          genis
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start genis at boot time
# Description:       Enable genis service.
### END INIT INFO

# Get genis env vars (CMD_OPTS)
. /etc/default/genis

name=`basename $0`
user="$name"
dir="/usr/share/$name"
pid_file="/var/run/$name/running.pid"
cmd="bin/genis"

stdout_log="/var/log/$name/service.log"
stderr_log="/var/log/$name/service.log"

do_start() {
    [ -d "/var/run/$name" ] || install -d -o "$name" -m750 "/var/run/$name"
    start-stop-daemon \
        --verbose \
        --start \
        --oknodo \
        --name "$name" \
        --user "$user" \
        --pidfile "$pid_file" \
        --chuid "$name" \
        --chdir "$dir" \
        --background \
        --no-close \
        --startas "$cmd" \
        -- -Dpidfile.path=$pid_file $CMD_OPTS >> "$stdout_log" 2>> "$stderr_log"
}

do_stop() {
    start-stop-daemon \
        --verbose \
        --stop \
        --oknodo \
        --user "$user" \
        --pidfile "$pid_file" 
}

case "$1" in
    start)
        do_start
    ;;
    stop)
        do_stop
    ;;
    restart)
        do_stop
        do_start
        exit $?
    ;;
    status)
        start-stop-daemon --status --pidfile "$pid_file"
        case "$?" in
        0)
          echo "genis is running"
          ;;
        1)
          echo "genis is not running and the pid file exists"
          ;;
        3)
          echo "genis is not running"
          ;;
        *)
          echo "Unable to determine genis status"
          ;;
        esac
    ;;
    *)
    echo "Usage: $0 {start|stop|restart|status}"
    exit 1
    ;;
esac

exit 0
