@echo off
java -Xmx2G -Xms2G -jar release/mi-lang.jar compile main=testing.main file='examples/testing.mi'
pause > nul