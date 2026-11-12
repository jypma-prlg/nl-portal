#!/bin/bash
echo ">>>>  NL Portal init script: Open Product <<<<"
until python /app/src/manage.py migrate --check >/dev/null 2>&1; do
    echo "Waiting for openproduct migrations..."
    sleep 5
done
python /app/src/manage.py load_upl --file /app/init/UPL-actueel.csv
python /app/src/manage.py loaddata /app/generated/configuration.json demodata
echo ">>>> Done <<<<"
