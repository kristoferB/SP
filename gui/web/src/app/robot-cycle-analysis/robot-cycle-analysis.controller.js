/**
 * Created by Daniel on 2016-04-29.
 */
(function () {
    'use strict';

    angular
      .module('app.robotCycleAnalysis')
      .controller('robotCycleAnalysisController', robotCycleAnalysisController);

    robotCycleAnalysisController.$inject = ['$scope', 'dashboardService', 'robotCycleAnalysisService'];
    /* @ngInject */
    function robotCycleAnalysisController($scope, dashboardService, robotCycleAnalysisService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.control = robotCycleAnalysisController;
        vm.connect = robotCycleAnalysisService.connectToBus;

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
                // maybe add some clean up here
            });
        }
        
    }
})();
