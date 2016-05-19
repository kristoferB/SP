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
        vm.myFunction = myFunction;
        vm.activate2 = activate2;

        vm.value = 1;
        vm.debug14 = 0;

        angular.module('staticSelect', [])
            .controller('Tobbe2Controller', ['$scope', function($scope) {
                $scope.data = {
                    singleSelect: null,
                    multipleSelect: [],
                    option1: "option-1"
                };
            }]);

        /* When the user clicks on the button,
         toggle between hiding and showing the dropdown content */
        function myFunction() {
            document.getElementById("myDropdown").classList.toggle("show");
        }

        // Close the dropdown menu if the user clicks outside of it
        window.onclick = function(event) {
            if (!event.target.matches('.dropbtn')) {

                var dropdowns = document.getElementsByClassName("dropdown-content");
                var i;
                for (i = 0; i < dropdowns.length; i++) {
                    var openDropdown = dropdowns[i];
                    if (openDropdown.classList.contains('show')) {
                        openDropdown.classList.remove('show');
                    }
                }
            }
        }

    resSel: {
        H1:         {
                        name: 'Elevator 1',
                        resource: [
                            {id: '135 0 0 true', action: 'Up'},
                            {id: '135 0 1 true', action: 'Down'}
                        ]
                    },
        H2:         {
                        name: 'Elevator 2',
                        resource: [
                            {id: '140 0 0 true', action: 'Up'},
                            {id: '140 0 1 true', action: 'Up'}
                        ]
                    },
        Flexlink:   {   
                        name: 'Flexlink',
                        resource: [
                            {id: '139 0 SAKNAS true', action: 'Start'},
                            {id: '139 0 SAKNAS true', action: 'Stop'}
                        ]
                    },
        R4:         {
                        name: 'Robot 4',
                        resource: [
                            {id: '128 0 2 true', action: 'Home'},
                            {id: '128 0 3 true', action: 'Dodge'}
                        ]

                    },
        R5:         {
                        name: 'Robot 4',
                        resource: [
                            {id: '132 0 2 true', action: 'Home'},
                            {id: '132 0 3 true', action: 'Dodge'}
                        ]

                    }
        };
    resMult: {    
        R2:         {
                        name: 'Robot 2',
                        resource: [
                            {id: '127 18 0 1', action: 'Set at position 1'},
                            {id: '127 18 0 2', action: 'Set at position 2'},
                            {id: '127 18 0 3', action: 'Set at position 3'},
                            {id: '127 18 0 4', action: 'Set at position 4'},
                            {id: '127 18 0 5', action: 'Set at position 5'},
                            {id: '127 0 5 true', action: 'Pick at set position'},
                            {id: '127 0 2 true', action: 'Place at elevator 2'},
                            {id: '127 0 6 true', action: 'Place at table'},
                        ]
        }
    };            

        function activate2(int) {
            if(int == 1)
                vm.debug14++;
            else
                vm.debug14--;
        }

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
