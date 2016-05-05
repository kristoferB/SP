/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.operatorInstGUI')
      .controller('operatorInstGUIController', operatorInstGUIController);

    operatorInstGUIController.$inject = ['$scope', 'dashboardService', 'eventService','spServicesService','restService','operatorInstGUIService'];
    /* @ngInject */
    function operatorInstGUIController($scope, dashboardService, eventService, spServicesService, restService,
                                       operatorInstGUIService) {
        var vm = this;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what
        vm.eventLog = [];
        vm.scope = $scope;
        vm.connect = connect;

        // bus connection stuff
        vm.connection = operatorInstGUIService;
        vm.busIP = '129.16.26.22';
        vm.publishTopic = 'commands';
        vm.subscribeTopic = 'response';

        function connect() {
            vm.connection.connect(vm.busIP,
                                 vm.publishTopic,
                                 vm.subscribeTopic);
        }

        vm.Palett = {
            pal: [
                    ["#fff","#fff","#fff","#fff"],
                    ["#fff","#fff","#fff","#fff"]
                 ],

            sendEmpty: [
                    ["empty","empty","empty","empty"],
                    ["empty","empty","empty","empty"]
                 ]
        };
        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }


    }
})();
