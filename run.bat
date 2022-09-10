@echo off
java -Xmx2G -Xms2G -jar release/mu.jar file='examples/rainbow.mu' main=testing.main pass=10 pass='hello, world!'
pause > nul