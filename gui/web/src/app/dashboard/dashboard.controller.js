(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['logger', '$state', '$timeout', 'dashboardService', '$rootScope', 'settingsService'];
    /* @ngInject */
    function DashboardController(logger, $state, $timeout, dashboardService, $rootScope, settingsService) {
        var vm = this;
        vm.dashboard = dashboardService.getDashboard(1);
        vm.title = $state.current.title;
       
        vm.gridsterOptions = {
            outerMargin: false,
            columns: 12,
            swapping: true,
            margins: [settingsService.getLayoutTheme().gridster_panel_margin,
                      settingsService.getLayoutTheme().gridster_panel_margin],
            draggable: {
                enabled: false,
                handle: '.panel-heading'
            }
        };

        vm.dashboardService = dashboardService;

        activate();

        function activate() {
            enableWidgetDrag();
            logger.log('Dashboard Controller: Activated Dashboard view.');
        }

        function enableWidgetDrag() {
            $timeout(function() {
                vm.gridsterOptions.draggable.enabled = true;
            }, 500, false);
        }

    }
})();
