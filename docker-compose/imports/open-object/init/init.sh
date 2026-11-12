#!/bin/bash
set -e
echo ">>>>  NL Portal init script: Open Object <<<<"
until python /app/src/manage.py migrate --check >/dev/null 2>&1; do
    echo "Waiting for open-object migrations..."
    sleep 5
done
python /app/src/manage.py loaddata /app/generated/configuration.json objecttypes objects
echo ">>>> Done <<<<"
