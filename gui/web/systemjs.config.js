/**
 * System configuration for Angular 2 samples
 * Adjust as necessary for your application needs.
 */
(function(global) {
  // map tells the System loader where to look for things
  var map = {
    //'app':                        'app', // 'dist',
    '@angular':                   'node_modules/@angular',
    'angular2-in-memory-web-api': 'node_modules/angular2-in-memory-web-api',
    'rxjs':                       'node_modules/rxjs',
    'd3':                         'node_modules/d3/build/d3.js',
    // ng2-bootstrap needs moment apparently
    'moment':                     'node_modules/moment/moment.js',
    'ng2-bootstrap':              'node_modules/ng2-bootstrap',
    'socket.io-client':           'node_modules/socket.io-client/socket.io.js',
    'angular2-grid':              'node_modules/angular2-grid/dist',
    'lodash':                     'node_modules/lodash/',
    'jsoneditor':                 'node_modules/jsoneditor/dist/'
  };
  // packages tells the System loader how to load when no filename and/or no extension
  var packages = {
    'ng2-bootstrap': { main: 'ng2-bootstrap.js', defaultExtension: 'js' },
    'angular2-grid': { main: 'main.js', defaultExtension: 'js' },
    'app':                        { main: 'main.js',  defaultExtension: 'js' },
    'rxjs':                       { defaultExtension: 'js' },
    'angular2-in-memory-web-api': { main: 'index.js', defaultExtension: 'js' },
    'lodash':                     { main: 'lodash.js', defaultExtension: 'js' },
    'jsoneditor':                 { main: 'jsoneditor.js', defaultExtension: 'js'}
    //'d3': { main: 'index.js', defaultExtension: 'js' }
  };
  var ngPackageNames = [
    'common',
    'compiler',
    'core',
    'forms',
    'http',
    'platform-browser',
    'platform-browser-dynamic',
    'router',
    'router-deprecated',
    'upgrade',
  ];
  // Individual files (~300 requests):
  function packIndex(pkgName) {
    packages['@angular/'+pkgName] = { main: 'index.js', defaultExtension: 'js' };
  }
  // Bundled (~40 requests):
  function packUmd(pkgName) {
    packages['@angular/'+pkgName] = { main: '/bundles/' + pkgName + '.umd.js', defaultExtension: 'js' };
  }
  // Most environments should use UMD; some (Karma) need the individual index files
  var setPackageConfig = System.packageWithIndex ? packIndex : packUmd;
  // Add package entries for angular packages
  ngPackageNames.forEach(setPackageConfig);
  var config = {
    map: map,
    packages: packages
  };
  System.config(config);
})(this);
