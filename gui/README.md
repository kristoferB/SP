## Prerequisites ##
You will need an sbt installation  (`scala-sbt.org/0.13/docs/`) 
You will also need an installation of node (`nodejs.org`) 

## Compiling the SP frontend ##

To install dependencies, cd to `SP/gui/npmdependencies` and run `npm install`.

To compile your code, cd to `SP/gui` and run sbt `fastOptJS`.
To see it, open `index.html` in a browser.

To compile the optimized version run `fullOptJS` (slow process, not recommended in development)
To see it, open `index-prod.html` in a browser.

To get automatic compilation on file change, run `~fastOptJS`.

## JavaScript dependencies #
JS dependencies are handled by npm and made available through a bundle file generated with webpack. To add a JS dependency, go to `npmdependencies/`, add it to `package.json` and `vendor.js`, then run `npm install`.
