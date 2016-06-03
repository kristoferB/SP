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
            eventService.eventSource.addEventListener('Response', onResponse);
            $scope.$on('modal.closing', function() {
                console.log("Work Cell modal is closing.");
                eventService.removeListener('Response', onResponse);
            });
            robotCycleAnalysisService.publishWorkCellListOpenedEvent();
        }

        function onResponse(ev) {
            let attrs = angular.fromJson(ev.data).attributes;
            console.log(attrs);
            if (_.has(attrs, 'workCells'))
                vm.workCells = attrs.workCells;
        }

        function select(selectedWorkCell) {
            $uibModalInstance.close(selectedWorkCell);
        }

    }
})();
