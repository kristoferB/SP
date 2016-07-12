"use strict";
var upgrade_1 = require('@angular/upgrade');
var adapter = new upgrade_1.UpgradeAdapter();
//angular.module('myApp')
//  .directive('heroDetail', adapter.downgradeNg2Component(HeroDetailComponent));
adapter.bootstrap(document.body, ['myApp']);
console.log("we here?");
//# sourceMappingURL=main.js.map