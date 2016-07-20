//import {Component} from '@angular/core';
import { AwesomeNG2Component } from './awesome-ng2-component.component';
import { Faces } from './erica-components/faces.component';
//import { upgradeAdapter } from './upgrade_adapter';
import {UpgradeAdapter} from '@angular/upgrade';
//import {bootstrap} from '@angular/platform-browser-dynamic';
/* . . . */

declare var angular: any;

var adapter: UpgradeAdapter = new UpgradeAdapter();

angular.module('app')
  .directive('awesomeNg2Component', adapter.downgradeNg2Component(AwesomeNG2Component))
  .directive('facesComponent', adapter.downgradeNg2Component(Faces));
adapter.bootstrap(document.documentElement, ['app']);
