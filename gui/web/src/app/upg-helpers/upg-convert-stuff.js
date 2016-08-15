"use strict";
var shell_component_1 = require('../layout/shell.component');
var ng2_dashboard_component_1 = require('../dashboard/ng2-dashboard.component');
var awesome_ng2_component_component_1 = require('../lazy-widgets/ng2Inside/awesome-ng2-component.component');
var faces_component_1 = require('../erica-components/faces.component');
var ng2_dashboard_service_1 = require("../dashboard/ng2-dashboard.service");
var theme_service_1 = require("../core/theme.service");
function upgConvertStuff(upgAdapter) {
    angular.module('app')
        .directive('shell', upgAdapter.downgradeNg2Component(shell_component_1.ShellComponent))
        .directive('ng2Dashboard', upgAdapter.downgradeNg2Component(ng2_dashboard_component_1.Ng2DashboardComponent))
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
    upgAdapter.upgradeNg1Provider('$http');
    upgAdapter.upgradeNg1Provider('$sessionStorage');
    upgAdapter.upgradeNg1Provider('$ocLazyLoad');
    upgAdapter.upgradeNg1Provider('themeService');
    upgAdapter.upgradeNg1Provider('themeService');
    upgAdapter.addProvider(ng2_dashboard_service_1.Ng2DashboardService);
    upgAdapter.addProvider(theme_service_1.ThemeService);
}
exports.upgConvertStuff = upgConvertStuff;
//# sourceMappingURL=upg-convert-stuff.js.map