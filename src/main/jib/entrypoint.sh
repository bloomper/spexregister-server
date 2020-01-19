#!/bin/sh

echo "The application will start in ${SLEEP_DELAY}s..." && sleep ${SLEEP_DELAY}
exec java ${JAVA_OPTS} -noverify -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom -cp /app/resources/:/app/classes/:/app/libs/* "nu.fgv.register.server.Application"  "$@"
