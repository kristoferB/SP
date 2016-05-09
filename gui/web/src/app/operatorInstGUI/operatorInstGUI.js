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
        vm.publishTopic = 'response';
        vm.subscribeTopic = 'commands';

        function connect() {
            vm.connection.connect(vm.busIP,
                                 vm.publishTopic,
                                 vm.subscribeTopic);
        }

        function getColor(pos) {
            if(_.isArray(vm.connection.operatorInstructions) &&
               pos < vm.connection.operatorInstructions.length) {
                var brick = vm.connection.operatorInstructions[pos];
                if(brick.color == 'Empty'  || brick.color == '0') return '#ffffff';
                if(brick.color == 'Yellow' || brick.color == '1') return '#aaff55';
                if(brick.color == 'Green'  || brick.color == '2') return '#66ce33';
                if(brick.color == 'Red'    || brick.color == '3') return '#cc3344';
                if(brick.color == 'Blue'   || brick.color == '4') return '#4433cc';
                console.log(brick);
            }
            return '#000000';
        }

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }


    }
})();
