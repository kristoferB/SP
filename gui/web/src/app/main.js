"use strict";
//import {Component} from '@angular/core';
var awesome_ng2_component_component_1 = require('./awesome-ng2-component.component');
var faces_component_1 = require('./erica-components/faces.component');
//import { upgradeAdapter } from './upgrade_adapter';
var upgrade_1 = require('@angular/upgrade');
var adapter = new upgrade_1.UpgradeAdapter();
angular.module('app')
    .directive('awesomeNg2Component', adapter.downgradeNg2Component(awesome_ng2_component_component_1.AwesomeNG2Component))
    .directive('facesComponent', adapter.downgradeNg2Component(faces_component_1.Faces));
adapter.bootstrap(document.documentElement, ['app']);
//# sourceMappingURL=main.js.map