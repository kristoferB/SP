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
        
        vm.chosenWorkCell = null;
        vm.control = RobotCycleAnalysisController;
        vm.historicalCycles = [];
        vm.liveEvents = [];
        vm.service = robotCycleAnalysisService;
        vm.setupBus = setupBus;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget;

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                // maybe add some clean up here
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('Response', onEvent);
            eventService.addListener('Progress', onEvent);
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
