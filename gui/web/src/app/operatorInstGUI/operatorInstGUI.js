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
        vm.getColor = getColor;

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

        function getColor(pos) {
            if(_.has(vm.connection.operatorInstructions.colors)) {
                var str = vm.connection.operatorInstructions.colors[pos];
                if(str == 'green') return '#66cc33';
                if(str == 'red') return '#cc3344';
                if(str == 'blue') return '#4433cc';
                if(str == 'empty') return '#fff';
            }
            return '#000';
        }

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }


    }
})();
