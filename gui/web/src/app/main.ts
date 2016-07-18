import { upgradeAdapter } from './upgrade_adapter';
import { Ng2AppComponent } from './layout/ng2-app.component';
import { AwesomeNG2Component } from './lazy-widgets/ng2Inside/awesome-ng2-component.component';
import { Faces } from './erica-components/faces.component';

declare var angular: any;

angular.module('app')
  .directive('ng2App', upgradeAdapter.downgradeNg2Component(Ng2AppComponent))
  .directive('awesomeNg2Component', upgradeAdapter.downgradeNg2Component(AwesomeNG2Component))
  .directive('facesComponent', upgradeAdapter.downgradeNg2Component(Faces));

upgradeAdapter.upgradeNg1Provider('config');
upgradeAdapter.upgradeNg1Provider('logger');
upgradeAdapter.upgradeNg1Provider('$document');
upgradeAdapter.upgradeNg1Provider('settingsService');

upgradeAdapter.bootstrap(document.documentElement, ['app']);
