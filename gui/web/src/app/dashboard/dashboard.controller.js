(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['logger', '$state', '$timeout', 'dashboardService', '$rootScope'];
    /* @ngInject */
    function DashboardController(logger, $state, $timeout, dashboardService, $rootScope) {
        var vm = this;
        dashboardService.getDashboard(1, function(dashboard) {
            vm.dashboard = dashboard;
        });
        vm.title = $state.current.title;
       
        vm.gridsterOptions = dashboardService.gridsterOptions;
        vm.dashboardService = dashboardService;

        activate();

        function activate() {
            logger.log('Dashboard Controller: Activated Dashboard view.');
        }

        function togglePanelLock() {
            $timeout(function() {
                vm.gridsterOptions.draggable.enabled = !vm.gridsterOptions.draggable.enabled ;
                vm.gridsterOptions.resizable.enabled = !vm.gridsterOptions.resizable.enabled ;
            }, 500, false);
        }
    }
})();
