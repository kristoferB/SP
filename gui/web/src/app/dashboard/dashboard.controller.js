(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['logger'];
    /* @ngInject */
    function DashboardController(logger) {
        var vm = this;
        vm.title = 'Dashboard';

        activate();

        function activate() {
            logger.info('Activated Dashboard View');
        }




    }
})();
