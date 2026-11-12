#!/bin/bash
echo ">>>>  NL Portal init script: Open Klant 2 <<<<"
until python /app/src/manage.py migrate --check >/dev/null 2>&1; do
    echo "Waiting for openklant migrations..."
    sleep 5
done
python /app/src/manage.py loaddata /app/generated/admin_user.json /app/generated/configuration.json partijen klantcontacten
echo ">>>> Done <<<<"
