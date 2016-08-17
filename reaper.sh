while true; do
  kill -9 $(jps -l | grep target/uberjar/competing-consumers-ordering-spike-0.1.0-SNAPSHOT-standalone.jar | cut -d ' ' -f1 | sort | head -n1);
  sleep 3;
done;
