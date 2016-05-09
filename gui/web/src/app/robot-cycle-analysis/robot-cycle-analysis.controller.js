/**
 * Created by Daniel on 2016-04-29.
 */
(function () {
    'use strict';

    angular
      .module('app.robotCycleAnalysis')
      .controller('RobotCycleAnalysisController', RobotCycleAnalysisController);

    RobotCycleAnalysisController.$inject = ['$scope', '$uibModal', 'dashboardService', 'robotCycleAnalysisService', 'eventService'];
    /* @ngInject */
    function RobotCycleAnalysisController($scope, $uibModal, dashboardService, robotCycleAnalysisService, eventService) {
        var vm = this;

        vm.addCycle = addCycle;
        vm.control = RobotCycleAnalysisController;
        vm.duration = duration;
        vm.liveEvents = [];
        vm.service = robotCycleAnalysisService;
        vm.setupBus = setupBus;
        vm.selectWorkCell = selectWorkCell;
        vm.timeFromCycleStart = timeFromCycleStart;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget;

        activate();

        function activate() {
            if (vm.widget.storage === undefined) {
                vm.widget.storage = {};
                vm.widget.storage.chosenWorkCell = null;
                vm.widget.storage.historicalCycles = [];
            }
            $scope.$on('closeRequest', function() {
                // maybe add some clean up here
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('Response', onResponse);
        }

        function onResponse(ev){
            var attrs = ev.attributes;
            if (_.has(attrs, 'robotCyclesResponse'))
                if (vm.widget.storage !== undefined &&
                    vm.widget.storage.chosenWorkCell !== undefined &&
                    attrs.robotCyclesResponse.workCellName === vm.widget.storage.chosenWorkCell.name) {
                    /*for (let cycle of attrs.robotCyclesResponse.foundCycles) {
                        for (let robot of cycle.events) {
                            for (let event of robot) {
                                event.time = new Date(event.time);
                            }
                        }
                        vm.widget.storage.historicalCycles.push(cycle);
                    }*/
                    vm.widget.storage.historicalCycles.push(...attrs.robotCyclesResponse.foundCycles);
                }
        }

        function setupBus() {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/robot-cycle-analysis/setup-bus.html',
                controller: 'SetupBusController',
                controllerAs: 'vm'
            });

            modalInstance.result.then(function(busSettings) {
                robotCycleAnalysisService.setupBus(busSettings);
            });
        }

        function duration(event) {
            let start = new Date(event.start);
            let stop = new Date(event.stop);
            let durationInMs = stop - start;
            return durationInMs / 1000;
        }
        
        function timeFromCycleStart(cycle, event) {
            let cycleStart = new Date(cycle.start);
            let eventStart = new Date(event.start);
            let durationInMs = eventStart - cycleStart;
            return durationInMs / 1000;
        }

        function addCycle() {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/robot-cycle-analysis/add-cycle.html',
                controller: 'AddCycleController',
                controllerAs: 'vm',
                resolve: {
                    workCell: function() {
                        return vm.widget.storage.chosenWorkCell;
                    }
                }
            });

            modalInstance.result.then(function(selectedCycleIds) {
                robotCycleAnalysisService.requestCycles(selectedCycleIds);
            });
        }

        function selectWorkCell() {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/robot-cycle-analysis/select-work-cell.html',
                controller: 'SelectWorkCellController',
                controllerAs: 'vm'
            });

            modalInstance.result.then(function(selectedWorkCell) {
                vm.widget.storage.chosenWorkCell = selectedWorkCell;
            });
        }

    }
})();
