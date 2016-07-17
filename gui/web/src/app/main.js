"use strict";
var platform_browser_dynamic_1 = require('@angular/platform-browser-dynamic');
var upgrade_adapter_1 = require('./upgrade_adapter');
var app_component_1 = require('./layout/app.component');
var awesome_ng2_component_component_1 = require('./awesome-ng2-component.component');
var faces_component_1 = require('./erica-components/faces.component');
angular.module('app')
    .directive('awesomeNg2Component', upgrade_adapter_1.upgradeAdapter.downgradeNg2Component(awesome_ng2_component_component_1.AwesomeNG2Component))
    .directive('facesComponent', upgrade_adapter_1.upgradeAdapter.downgradeNg2Component(faces_component_1.Faces));
upgrade_adapter_1.upgradeAdapter.bootstrap(document.documentElement, ['app']);
platform_browser_dynamic_1.bootstrap(app_component_1.AppComponent);
//# sourceMappingURL=main.js.map