/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.Tobbe2')
      .controller('Tobbe2Controller', Tobbe2Controller);

    Tobbe2Controller.$inject = ['$scope', 'dashboardService', 'eventService','spServicesService'];
    /* @ngInject */
    function Tobbe2Controller($scope, dashboardService, eventService,spServicesService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what

        //functions
        vm.sendOrder = sendOrder;
        activate();

        vm.value = 1;


        angular.module('staticSelect', [])
            .controller('Tobbe2Controller', ['$scope', function($scope) {
                $scope.data = {
                    singleSelect: null,
                    multipleSelect: [],
                    option1: "option-1"
                };
            }]);


        function activate() {
            $scope.$on('closeRequest', function () {
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
        }


        function onEvent(ev) {
            console.log("SensorGUI Test");
            console.log(ev);

            if (_.has(ev, 'attributes.stateWithName')) {
                //service.stateWithName = ev.attributes.stateWithName;
                if (ev.attributes.stateWithName.name.equals("IH2.mode")) {
                    vm.value = ev.attributes.stateWithName.value;
                }
            }

            /*
            if (!_.has(ev, 'reqID') || ev.reqID !== service.controlServiceID) return;

            if (_.has(ev, 'attributes.theBus')){
                if (ev.attributes.theBus === 'Connected' && ! service.connected){
                    sendTo(service.latestMess, 'subscribe');
                }
                service.connected = ev.attributes.theBus === 'Connected'
            }

            if (_.has(ev, 'attributes.state')){
                service.state = ev.attributes.state;
            }
            if (_.has(ev, 'attributes.resourceTree')){
                service.resourceTree = ev.attributes.resourceTree;
            }

            */
        }

        function sendOrder() {

            var mess = {"data": {getNext: false, "buildOrder": vm.ButtonColour.kub}};
            spServicesService.callService(spServicesService.getService("operatorService"),
                mess,
                function (resp) {
                    if (_.has(resp, 'attributes.result')) {
                        console.log("Hej" + vm.result);
                    }
                }
            )
        }
    }
})();
