#! /bin/bash

username="$USER"

name=$1
host=$2
port=$3
zkHost=$4
zkPort=$5

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# Copy ssh key over with `ssh-copy-id user@host`
ssh -n "$username"@"$host" kill -9 $(lsof -t -i:"$port") &> /dev/null
ssh -n "$username"@"$host" nohup java -jar "$DIR"/m3-server.jar "$name" "$port" "$zkHost" "$zkPort"