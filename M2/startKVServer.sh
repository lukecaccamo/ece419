#! /bin/bash

filename="id_rsa"
path="$HOME/.ssh"
username="$USER"

host=$(hostname) # $1
port=$2
cacheSize=$3
cacheStrategy=$4

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
  ssh -n "$host" nohup java -jar "$DIR"/m2-server.jar "$port" "$cacheSize" "$cacheStrategy"
} 2> /dev/null