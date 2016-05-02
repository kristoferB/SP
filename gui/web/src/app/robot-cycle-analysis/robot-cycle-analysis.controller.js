/**
 * Created by Daniel on 2016-04-29.
 */
(function () {
    'use strict';

    angular
      .module('app.robotCycleAnalysis')
      .controller('RobotCycleAnalysisController', RobotCycleAnalysisController);

    RobotCycleAnalysisController.$inject = ['$scope', 'dashboardService', 'robotCycleAnalysisService', 'eventService', '$uibModal'];
    /* @ngInject */
    function RobotCycleAnalysisController($scope, dashboardService, robotCycleAnalysisService, eventService, $uibModal) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.control = RobotCycleAnalysisController;

        vm.connectToBus = robotCycleAnalysisService.connectToBus;
        vm.disconnectFromBus = robotCycleAnalysisService.disconnectFromBus;
        vm.setupBus = setupBus;
        vm.chosenWorkCell = null;
        vm.historicalCycles = [];
        vm.liveEvents = [];

        activate();

        function activate() {
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
                controllerAs: 'vm',
                resolve: {
                    robotCycleAnalysisService: function () {
                        return robotCycleAnalysisService;
                    }
                }
            });

            modalInstance.result.then(function(busSettings) {
                robotCycleAnalysisService.setupBus(busSettings);
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
