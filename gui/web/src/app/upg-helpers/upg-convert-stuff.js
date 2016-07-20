"use strict";
var shell_component_1 = require('../layout/shell.component');
var awesome_ng2_component_component_1 = require('../lazy-widgets/ng2Inside/awesome-ng2-component.component');
var faces_component_1 = require('../erica-components/faces.component');
function upgConvertStuff(upgAdapter) {
    angular.module('app')
        .directive('shell', upgAdapter.downgradeNg2Component(shell_component_1.ShellComponent))
        .directive('awesomeNg2Component', upgAdapter.downgradeNg2Component(awesome_ng2_component_component_1.AwesomeNG2Component))
        .directive('facesComponent', upgAdapter.downgradeNg2Component(faces_component_1.Faces));
    upgAdapter.upgradeNg1Provider('config');
    upgAdapter.upgradeNg1Provider('logger');
    upgAdapter.upgradeNg1Provider('$document');
    upgAdapter.upgradeNg1Provider('settingsService');
    upgAdapter.upgradeNg1Provider('modelService');
    upgAdapter.upgradeNg1Provider('dashboardService');
    upgAdapter.upgradeNg1Provider('widgetListService');
    upgAdapter.upgradeNg1Provider('$state');
    upgAdapter.upgradeNg1Provider('$uibModal');
}
exports.upgConvertStuff = upgConvertStuff;
//# sourceMappingURL=upg-convert-stuff.js.map