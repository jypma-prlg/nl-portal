#!/bin/bash
set -e
echo ">>>>  NL Portal init script: Open Notificaties <<<<"
until python /app/src/manage.py migrate --check >/dev/null 2>&1; do
    echo "Waiting for opennotificaties migrations..."
    sleep 5
done
python /app/src/manage.py loaddata configuration kanalen
echo ">>>> Done <<<<"
