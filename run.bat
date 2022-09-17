@echo off
java -Xmx2G -Xms2G -jar release/mu.jar file='examples/recursion.mu' main=recursion.main
pause > nul