"use strict";
var upgrade_adapter_1 = require('./upgrade_adapter');
var ng2_app_component_1 = require('./layout/ng2-app.component');
var awesome_ng2_component_component_1 = require('./awesome-ng2-component.component');
var faces_component_1 = require('./erica-components/faces.component');
angular.module('app')
    .directive('ng2App', upgrade_adapter_1.upgradeAdapter.downgradeNg2Component(ng2_app_component_1.Ng2AppComponent))
    .directive('awesomeNg2Component', upgrade_adapter_1.upgradeAdapter.downgradeNg2Component(awesome_ng2_component_component_1.AwesomeNG2Component))
    .directive('facesComponent', upgrade_adapter_1.upgradeAdapter.downgradeNg2Component(faces_component_1.Faces));
upgrade_adapter_1.upgradeAdapter.upgradeNg1Provider('config');
upgrade_adapter_1.upgradeAdapter.upgradeNg1Provider('logger');
upgrade_adapter_1.upgradeAdapter.upgradeNg1Provider('$document');
upgrade_adapter_1.upgradeAdapter.upgradeNg1Provider('settingsService');
upgrade_adapter_1.upgradeAdapter.bootstrap(document.documentElement, ['app']);
//# sourceMappingURL=main.js.map