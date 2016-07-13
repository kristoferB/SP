"use strict";
//import {Component} from '@angular/core';
var awesome_ng2_component_component_1 = require('./awesome-ng2-component.component');
//import { upgradeAdapter } from './upgrade_adapter';
var upgrade_1 = require('@angular/upgrade');
var adapter = new upgrade_1.UpgradeAdapter();
angular.module('app')
    .directive('awesomeNg2Component', adapter.downgradeNg2Component(awesome_ng2_component_component_1.AwesomeNG2Component));
adapter.bootstrap(document.documentElement, ['app']);
//# sourceMappingURL=main.js.map