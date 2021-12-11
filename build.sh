#!/bin/bash
echo "Running func tests"
echo "Starting server"

trap 'kill -TERM $SERVER_PID' TERM INT
./target/universal/stage/bin/scala-pet-store &

SERVER_PID=$!
PARENT_PID=$$

echo "Waiting for app to start...server pid is $SERVER_PID; parent pid is $PARENT_PID"
DATA=""
RETRY=30

while [ $RETRY -gt 0 ]
do
    DATA=$(nc -v -z localhost 8080)
    if [ $? -eq 0 ]
    then
        break
    else
        echo "Retrying Again" >&2

        let RETRY-=1
        sleep 1

        if [ $RETRY -eq 0 ]
        then
          echo "Exceeded retries waiting for app to be ready, failing"
          exit 1
        fi
    fi
done

kill $SERVER_PID
wait $SERVER_PID
trap - TERM INT
wait $SERVER_PID
# kill -9 $PARENT_PID

echo "DONE!"
exit $FUNC_TEST_RESULT

