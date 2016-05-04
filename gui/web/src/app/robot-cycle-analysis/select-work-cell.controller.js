(function () {
    'use strict';

    angular
        .module('app.robotCycleAnalysis')
        .controller('SelectWorkCellController', SelectWorkCellController);

    SelectWorkCellController.$inject = ['$uibModalInstance', 'robotCycleAnalysisService'];
    /* @ngInject */
    function SelectWorkCellController($uibModalInstance, robotCycleAnalysisService) {
        var vm = this;
        vm.select = select;
        vm.cancel = cancel;
        vm.service = robotCycleAnalysisService;
        
        activate();

        function activate() {
            robotCycleAnalysisService.requestAvailableWorkCells();
        }

        function select(selectedWorkCell) {
            $uibModalInstance.close(selectedWorkCell);
        }

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }

    }
})();
