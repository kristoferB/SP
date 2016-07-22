import { UpgradeAdapter } from '@angular/upgrade';

import { ShellComponent } from '../layout/shell.component';
import { AwesomeNG2Component } from '../lazy-widgets/ng2Inside/awesome-ng2-component.component';
import { Faces } from '../erica-components/faces.component';

declare var angular: any;

export function upgConvertStuff(upgAdapter: UpgradeAdapter): void {

    angular.module('app')
      .directive('shell',
                 upgAdapter.downgradeNg2Component(ShellComponent))
      .directive('awesomeNg2Component',
                 upgAdapter.downgradeNg2Component(AwesomeNG2Component))
      .directive('facesComponent',
                 upgAdapter.downgradeNg2Component(Faces));

    upgAdapter.upgradeNg1Provider('config');
    upgAdapter.upgradeNg1Provider('logger');
    upgAdapter.upgradeNg1Provider('$document');
    upgAdapter.upgradeNg1Provider('settingsService');
    upgAdapter.upgradeNg1Provider('modelService');
    upgAdapter.upgradeNg1Provider('dashboardService');
    upgAdapter.upgradeNg1Provider('widgetListService');
    upgAdapter.upgradeNg1Provider('$state');
    upgAdapter.upgradeNg1Provider('$uibModal');
    upgAdapter.upgradeNg1Provider('themeService');
}
