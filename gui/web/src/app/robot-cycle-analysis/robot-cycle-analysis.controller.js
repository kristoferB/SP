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
        vm.historicalCycles = [];
        vm.liveEvents = [];
        vm.service = robotCycleAnalysisService;
        vm.setupBus = setupBus;
        vm.selectWorkCell = selectWorkCell;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget;

        activate();

        function activate() {
            if (vm.widget.storage === undefined) {
                vm.widget.storage = {};
                vm.widget.storage.chosenWorkCell = null;
            }
            $scope.$on('closeRequest', function() {
                // maybe add some clean up here
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('Response', onEvent);
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

            modalInstance.result.then(function(selectedCycle) {

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

        function onEvent(ev){
            if (_.has(ev, 'availableCycles'))
                vm.state.availableCycles = ev.availableCycles;
            if (_.has(ev, 'cycle'))
                vm.historicalCycles.push(ev.cycle);
            if (_.has(ev, 'cycleEvent'))
                vm.liveEvents.push(ev.cycleEvent);
        }

    }
})();
