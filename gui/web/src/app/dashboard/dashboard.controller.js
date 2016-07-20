(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['logger', '$state', '$timeout', 'dashboardService', '$rootScope'];
    /* @ngInject */
    function DashboardController(logger, $state, $timeout, dashboardService, $rootScope) {
        var vm = this;
        vm.dashboard = dashboardService.getDashboard(1);
        vm.title = $state.current.title;
       
        vm.gridsterOptions = dashboardService.gridsterOptions;
        vm.dashboardService = dashboardService;

        activate();

        function activate() {
            logger.log('Dashboard Controller: Activated Dashboard view.');
        }
    }
})();
