(function () {
    'use strict';

    angular
        .module('app.robotCycleAnalysis')
        .controller('SelectWorkCellController', SelectWorkCellController);

    SelectWorkCellController.$inject = ['$uibModalInstance', 'robotCycleAnalysisService', 'eventService', '$scope'];
    /* @ngInject */
    function SelectWorkCellController($uibModalInstance, robotCycleAnalysisService, eventService, $scope) {
        var vm = this;

        vm.workCells = null;
        vm.select = select;

        activate();

        function activate() {
            eventService.addListener('Response', onResponse);
            $scope.$on('modal.closing', function() {
                eventService.removeListener('Response', onResponse);
            });
            robotCycleAnalysisService.publishWorkCellListOpenedEvent();
        }

        function onResponse(ev) {
            var attrs = ev.attributes;
            if (_.has(attrs, 'workCells'))
                vm.workCells = attrs.workCells;
        }

        function select(selectedWorkCell) {
            $uibModalInstance.close(selectedWorkCell);
        }

    }
})();
