"use strict";
//import {Component} from '@angular/core';
var hero_detail_component_1 = require('./hero-detail.component');
//import { upgradeAdapter } from './upgrade_adapter';
var upgrade_1 = require('@angular/upgrade');
var adapter = new upgrade_1.UpgradeAdapter();
angular.module('myApp')
    .directive('heroDetail', adapter.downgradeNg2Component(hero_detail_component_1.HeroDetailComponent));
console.log("we here?");
adapter.bootstrap(document.body, ['myApp']);
//# sourceMappingURL=main.js.map