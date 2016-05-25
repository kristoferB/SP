/**
 * Created by Martin on 2016-05-24.
 */
(function () {
    'use strict';

    angular
      .module('app.processSimulate')
      .controller('processSimulateController', processSimulateController);

    processSimulateController.$inject = ['$scope', 'dashboardService','logger', 'modelService','itemService','restService','eventService', 'processSimulateService'];
    /* @ngInject */
    function processSimulateController($scope, dashboardService, logger, modelService,itemService,restService,eventService, processSimulateService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.ps = processSimulateService;
        vm.items = itemService.items;

        vm.connect = connect;
        vm.connectedMessage = 'Not connected';

        vm.serviceName = 'ProcessSimulate';
        vm.busIP = 'localhost';
        vm.topic = 'ps';

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
                // maybe add some clean up here
            });

        }

        function connect(){
            processSimulateService.connect({
                'ip':vm.busIP,
                'topic':vm.topic
            }, vm.connectionDetailsID, vm.resourcesID);
            vm.connected = true;
        }
    }
})();
