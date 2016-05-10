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

        function getColor(row,col) {
            if(_.isArray(vm.connection.operatorInstructions)) {
                var brick = _.find(vm.connection.operatorInstructions,function(b) {
                    return b.row == row && b.col == col;
                });
                if(_.isUndefined(brick)) return '#ffffff'; // empty
                var color = brick.color;
                if(color == 'Yellow' || color == '1') return '#aaff55';
                if(color == 'Green'  || color == '2') return '#66ce33';
                if(color == 'Red'    || color == '3') return '#cc3344';
                if(color == 'Blue'   || color == '4') return '#4433cc';
            }
            return '#000000'; // error
        }

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }


    }
})();
