#!/bin/bash
set -e
echo ">>>>  NL Portal init script: Open Klant 2 <<<<"
until python /app/src/manage.py migrate --check >/dev/null 2>&1; do
    echo "Waiting for openklant migrations..."
    sleep 5
done
python /app/src/manage.py loaddata admin_user configuration partijen klantcontacten
echo ">>>> Done <<<<"
