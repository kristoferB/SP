/**
 * Created by Daniel on 2016-04-29.
 */
(function () {
    'use strict';

    angular
      .module('app.robotCycleAnalysis')
      .controller('robotCycleAnalysisController', robotCycleAnalysisController);

    robotCycleAnalysisController.$inject = ['$scope', 'dashboardService','logger', 'modelService','itemService','restService','eventService', 'robotCycleAnalysisService'];
    /* @ngInject */
    function robotCycleAnalysisController($scope, dashboardService, logger, modelService, itemService, restService, eventService, robotCycleAnalysisService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.control = robotCycleAnalysisController;
        
        vm.connect = connect;
        
        vm.serviceID = '';
        vm.serviceName = 'RobotCycleAnalysis';

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
                // maybe add some clean up here

            });
        }
        
        function connect(){
            robotCycleAnalysisService.connect({
                'ip':vm.busIP,
                'publish':vm.publishTopic,
                'subscribe': vm.subscribeTopic
            }, vm.connectionDetailsID, vm.resourcesID);

            vm.connected = true;
        }
    }
})();
