(function () {
    'use strict';

    angular
        .module('app.robotCycleAnalysis')
        .controller('SearchCycleController', SearchCycleController);

    SearchCycleController.$inject = ['$uibModalInstance', 'robotCycleAnalysisService', 'workCell', 'eventService', '$scope'];
    /* @ngInject */
    function SearchCycleController($uibModalInstance, robotCycleAnalysisService, workCell, eventService, $scope) {
        var vm = this;
        
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
        vm.select = select;

        activate();
        
        function activate() {
            eventService.addListener('Response', onResponse);
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
            var attrs = ev.attributes;
            if (_.has(attrs, 'robotCycleSearchResult') && attrs.robotCycleSearchResult.workCellId === workCell.id) {
                vm.foundCycles = attrs.robotCycleSearchResult.foundCycles;
            }
        }

        function search() {
            vm.foundCycles = null;
            robotCycleAnalysisService.searchCycles(vm.searchQuery);
        }

        function select(selectedCycle) {
            $uibModalInstance.close([selectedCycle]);
        }

    }
})();
