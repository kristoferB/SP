# SP Frontend #
To install:
```npm install
sbt compile
```
To run the server:
```npm start
sbt ~fastOptJS
```
In this test version you will also have to copy the output from sp-scalajs-widget/build into the build folder in this directory.

# Test Widget #
To compile:
```sbt ~fastOptJS
```
You have to manually copy you widget to somewhere where the server can find it. Currently, do this by copying the javascript output in sp-scalajs-widget/build into sp-core/build.