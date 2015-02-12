#!/bin/bash

echo 'Generating package...'

pack_path=$(activator dist | tee /dev/tty | awk '/package is ready/ {print $7;}')

package_path=${pack_path%????}

fullfile=$(basename ${package_path})

filename=${fullfile%????}

echo "Package is located at ${package_path}"

server_ips="54.69.177.193"

echo 'Starting deployment...'

if [ -e "$package_path" ]; then
    for server in "$server_ips"
    do
        echo "Deploying to ${server}."
        scp "$package_path" ubuntu@${server}:/home/ubuntu/cillo-api.zip
        ssh ubuntu@${server} bash -c "'
            unzip cillo-api.zip
            chmod 755 ./${filename}/bin/cillo-api
            rm cillo-api.zip
        '"
        ssh ubuntu@${server} "eval kill \$(ps aux | grep [c]illo | awk '{print \$2}')"
        ssh ubuntu@${server} bash -c "'
            ./${filename}/bin/cillo-api -J-Xms128M -J-Xmx512m -J-server -Dconfig.file=/home/ubuntu/prod.conf &
            disown
        '"
    done
else
    echo 'Deployment error.'
fi
