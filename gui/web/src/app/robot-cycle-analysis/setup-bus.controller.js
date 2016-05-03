(function () {
    'use strict';

    angular
        .module('app.robotCycleAnalysis')
        .controller('SetupBusController', SetupBusController);

    SetupBusController.$inject = ['$uibModalInstance', 'robotCycleAnalysisService'];
    /* @ngInject */
    function SetupBusController($uibModalInstance, robotCycleAnalysisService) {
        var vm = this;
        vm.busSettings = {};
        vm.save = save;
        vm.cancel = cancel;
        
        activate();

        function activate() {
            angular.copy(robotCycleAnalysisService.state.busSettings, vm.busSettings);
        }

        function save() {
            $uibModalInstance.close(vm.busSettings);
        }

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }

    }
})();
