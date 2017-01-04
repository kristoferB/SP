# SP Frontend #
To install:
```
cd SP/gui/npmdependencies
npm install
cd ..
sbt fastOptJS
```
To see it, open `index.html` in a browser.

To install and see the optimized version:
```
cd SP/gui/npmdependencies
npm install
npm run build
cd ..
sbt fullOptJS
```
Then open `index-prod.html` in a browser.

## JavaScript dependencies #
JS dependencies are handled by npm and made available through a bundle file generated with webpack. To add a JS dependency, go to `npmdependencies/`, add it to `package.json` and `vendor.js`, then run `npm install`.
