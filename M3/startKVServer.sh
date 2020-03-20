#! /bin/bash

filename="id_rsa"
path="$HOME/.ssh"
username="$USER"

name=$1
zkHost=$2
zkPort=$3
host=$(hostname) # $4
port=$5
cacheSize=$6
cacheStrategy=$7

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"

  # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

{
  ssh -n "$host" kill -9 $(lsof -t -i:"$port") &> /dev/null
  ssh -n "$host" nohup java -jar "$DIR"/m3-server.jar "$name" "$zkHost" "$zkPort" "$port" "$cacheSize" "$cacheStrategy"
} 2> /dev/null