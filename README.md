# gatewaykit

NOTE!!!
Intellij update caused build issues for 20 minutes
AND
Windows update took 10 minutes

Another 1/2 hour and I would have had GET routes working


To set up build:
1. Use Intellij with dependent libraries
2. Update the build.gradle file accordingly
3. Add VM argument with path to the config file (-DGATEWAY_FILE=gateway.yaml)
4. Run the stand-alone JAR with the following:
   java -jar gatewaykit-1.0.1-SNAPSHOT.jar


Next Planned steps
* Add test for happy-path and error cases
* Add rate limiting according to yaml parameters





