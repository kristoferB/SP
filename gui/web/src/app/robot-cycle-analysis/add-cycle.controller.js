(function () {
    'use strict';

    angular
        .module('app.robotCycleAnalysis')
        .controller('AddCycleController', AddCycleController);

    AddCycleController.$inject = ['$uibModalInstance', 'robotCycleAnalysisService', 'workCell', 'eventService', '$scope'];
    /* @ngInject */
    function AddCycleController($uibModalInstance, robotCycleAnalysisService, workCell, eventService, $scope) {
        var vm = this;
        vm.cancel = cancel;
        vm.cycleSearchResult = null;
        vm.search = search;
        vm.searchQuery = {
            cycle: {
                id: null
            },
            timeSpan: {
                start: null,
                stop: null
            },
            workCell: workCell
        };
        vm.select = select;
        
        activate();

        function activate() {
            eventService.addListener('Response', onResponse);
            $scope.$on('modal.closing', function(event, data) {
                eventService.removeListener('Response', onResponse);
            })
        }

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }

        function onResponse(ev) {
            var attrs = ev.attributes;
            if (_.has(attrs, 'cycleSearchResult') && _.has(attrs, 'workCell') && attrs.workCell.name === workCell.name) {
                vm.cycleSearchResult = attrs.cycleSearchResult;
            }
        }

        function search() {
            vm.foundCycles = null;
            robotCycleAnalysisService.searchCycles(searchQuery);
        }

        function select(selectedCycle) {
            $uibModalInstance.close(selectedCycle);
        }

    }
})();
