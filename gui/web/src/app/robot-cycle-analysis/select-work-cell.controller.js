(function () {
    'use strict';

    angular
        .module('app.robotCycleAnalysis')
        .controller('SelectWorkCellController', SelectWorkCellController);

    SelectWorkCellController.$inject = ['$uibModalInstance', 'robotCycleAnalysisService', 'eventService', '$scope'];
    /* @ngInject */
    function SelectWorkCellController($uibModalInstance, robotCycleAnalysisService, eventService, $scope) {
        var vm = this;

        vm.availableWorkCells = null;
        vm.select = select;

        activate();

        function activate() {
            eventService.addListener('Response', onResponse);
            $scope.$on('modal.closing', function() {
                eventService.removeListener('Response', onResponse);
            });
            robotCycleAnalysisService.requestAvailableWorkCells();
        }

        function onResponse(ev) {
            var attrs = ev.attributes;
            if (_.has(attrs, 'availableWorkCells'))
                vm.availableWorkCells = attrs.availableWorkCells;
        }

        function select(selectedWorkCell) {
            $uibModalInstance.close(selectedWorkCell);
        }

    }
})();
