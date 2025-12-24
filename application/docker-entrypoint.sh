#!/bin/sh
set -e

JAR_PATH=${JAR_PATH:-/app/app.jar}
APP_CDS_ENABLED=${APP_CDS_ENABLED:-true}
APP_CDS_ARCHIVE=${APP_CDS_ARCHIVE:-/app/app-cds.jsa}
SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}
APP_CDS_STRICT=${APP_CDS_STRICT:-false} # if true, fail when AppCDS not available

if [ "${APP_CDS_ENABLED}" = "true" ] && [ -f "${APP_CDS_ARCHIVE}" ]; then
  echo "Starting with AppCDS archive ${APP_CDS_ARCHIVE}"
  if java -Xshare:on -XX:SharedArchiveFile="${APP_CDS_ARCHIVE}" ${JAVA_OPTS} -jar "${JAR_PATH}"; then
    exit 0
  fi
  echo "AppCDS failed to start; falling back to normal startup."
  if [ "${APP_CDS_STRICT}" = "true" ]; then
    echo "APP_CDS_STRICT=true is set; exiting."
    exit 1
  fi
fi

echo "Starting without AppCDS."
exec java ${JAVA_OPTS} -jar "${JAR_PATH}"