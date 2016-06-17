(function () {
    'use strict';

    angular
        .module('app.robotCycleAnalysis')
        .controller('SearchCycleController', SearchCycleController);

    SearchCycleController.$inject = ['$uibModalInstance', 'robotCycleAnalysisService', 'workCell', 'eventService', '$scope', 'addCycle'];
    /* @ngInject */
    function SearchCycleController($uibModalInstance, robotCycleAnalysisService, workCell, eventService, $scope, addCycle) {
        var vm = this;

        vm.addCycle = addCycle;
        vm.foundCycles = null;
        vm.search = search;
        vm.searchQuery = {
            cycleId: null,
            timeSpan: {
                from: getCurrentDateMinusOneHour(),
                to: getCurrentDate()
            },
            workCellId: workCell.id
        };
        
        activate();

        function activate() {
            eventService.eventSource.addEventListener('Response', onResponse);
            $scope.$on('modal.closing', function() {
                eventService.removeListener('Response', onResponse);
            })
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
            let attrs = angular.fromJson(ev.data).attributes;
            if (_.has(attrs, 'foundCycles') && attrs.workCellId === workCell.id) {
                vm.foundCycles = attrs.foundCycles;
                $scope.$apply();
            }
        }

        function search() {
            vm.foundCycles = null;
            robotCycleAnalysisService.searchCycles(vm.searchQuery);
        }

    }
})();
