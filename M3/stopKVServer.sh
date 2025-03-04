#! /bin/bash

username="$USER"

host=$1
port=$2

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# Copy ssh key over with `ssh-copy-id user@host`
ssh -n "$username"@"$host" kill -9 $(lsof -t -i:"$port") &> /dev/null