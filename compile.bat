@echo off
java -Xmx2G -Xms2G -jar release/mi-lang.jar compile file='examples/rainbow.mi'
pause > nul