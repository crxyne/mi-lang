@echo off
java -Xmx2G -Xms2G -jar release/mu.jar file='examples/helloworld.mu' main=testing.main
pause > nul