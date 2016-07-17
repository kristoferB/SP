import {bootstrap} from '@angular/platform-browser-dynamic';

import { upgradeAdapter } from './upgrade_adapter';
import { AppComponent } from './layout/app.component';
import { AwesomeNG2Component } from './awesome-ng2-component.component';
import { Faces } from './erica-components/faces.component';

declare var angular: any;

angular.module('app')
  .directive('awesomeNg2Component', upgradeAdapter.downgradeNg2Component(AwesomeNG2Component))
  .directive('facesComponent', upgradeAdapter.downgradeNg2Component(Faces));
upgradeAdapter.bootstrap(document.documentElement, ['app']);

bootstrap(AppComponent);
