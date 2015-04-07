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
        rsync -avz $HOME/.cillo/prod_api.conf ubuntu@${server}:/home/ubuntu/
        ssh ubuntu@${server} bash -c "'
            unzip -o cillo-api.zip
            chmod 755 ./${filename}/bin/cillo
            rm cillo-api.zip
        '"
        ssh ubuntu@${server} "eval sudo kill \$(ps aux | grep [c]illo | awk '{print \$2}')"
        ssh ubuntu@${server} bash -c "'
            sudo ./${filename}/bin/cillo -J-Xms128M -J-Xmx512m -J-server -Dconfig.file=/home/ubuntu/prod_api.conf -Dhttp.port=80 &
            disown
        '"
    done
else
    echo 'Deployment error.'
fi
