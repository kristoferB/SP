(function () {
    'use strict';

    angular
        .module('app.robotCycleAnalysis')
        .controller('SetLiveChartWidthController', SetLiveChartWidthController);

    SetLiveChartWidthController.$inject = ['$uibModalInstance', 'width'];
    /* @ngInject */
    function SetLiveChartWidthController($uibModalInstance, width) {
        var vm = this;
        vm.width = width;
        vm.set = set;
        vm.cancel = cancel;
        
        activate();

        function activate() {
            
        }

        function set() {
            $uibModalInstance.close(vm.width);
        }

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }

    }
})();
