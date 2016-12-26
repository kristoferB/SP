# SP Frontend #
To install:
```
cd gui/npmdependencies
npm install
cd ..
sbt fastOptJS
```
To see it, open `index.html` in a browser.

## JavaScript dependencies #
JS dependencies are handled by npm and made available through a bundle file generated with webpack. To add a JS dependency, go to `npmdependencies`, add it to `package.json` and `vendor.js`, then run `npm install`.


# Test Widget #
To compile:
```
sbt ~fastOptJS
```
You have to manually copy you widget to somewhere where the server can find it. Currently, do this by copying the javascript output in sp-scalajs-widget/build into sp-core/build.
