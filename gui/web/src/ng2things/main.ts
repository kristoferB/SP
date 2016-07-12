//import {Component} from '@angular/core';
import { HeroDetailComponent } from './hero-detail.component';
//import { upgradeAdapter } from './upgrade_adapter';
import {UpgradeAdapter} from '@angular/upgrade';
//import {bootstrap} from '@angular/platform-browser-dynamic';
/* . . . */

declare var angular: any;

var adapter: UpgradeAdapter = new UpgradeAdapter();

angular.module('myApp')
  .directive('heroDetail', adapter.downgradeNg2Component(HeroDetailComponent));
console.log("we here?");
adapter.bootstrap(document.body, ['myApp']);
