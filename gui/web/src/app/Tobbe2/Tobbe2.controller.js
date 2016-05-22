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
        vm.dropdownList = dropdownList;

        vm.selectedDropdown = null;
        vm.value = 1;
        vm.debug14 = 0;

        vm.dropdownListEnum = {
            H1 : 0,
            H2 : 1,
            Table : 2
        }

        function dropdownList(string){
            vm.selectedDropdown = vm.dropdownListEnum;
        }

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
