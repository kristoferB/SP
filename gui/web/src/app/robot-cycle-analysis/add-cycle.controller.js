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
                from: getCurrentDateMinusOneHour(),
                to: getCurrentDate()
            },
            workCell: workCell
        };
        vm.select = select;

        activate();



        function activate() {
            eventService.addListener('Response', onResponse);
            $scope.$on('modal.closing', function() {
                eventService.removeListener('Response', onResponse);
            })
        }

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }

        function getCurrentDate() {
            var date = new Date();
            date.setSeconds(0);
            date.setMilliseconds(0);
            return date;
        }

        function getCurrentDateMinusOneHour() {
            var date = getCurrentDate();
            date.setHours(date.getHours() - 1);
            return date;
        }

        function onResponse(ev) {
            var attrs = ev.attributes;
            if (_.has(attrs, 'cycleSearchResult') && _.has(attrs, 'workCell') && attrs.workCell.name === workCell.name) {
                vm.cycleSearchResult = attrs.cycleSearchResult;
            }
        }

        function search() {
            vm.foundCycles = null;
            robotCycleAnalysisService.searchCycles(vm.searchQuery);
        }

        function select(selectedCycle) {
            $uibModalInstance.close(selectedCycle);
        }

    }
})();
