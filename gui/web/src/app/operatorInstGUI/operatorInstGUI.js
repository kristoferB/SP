/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.operatorInstGUI')
      .controller('operatorInstGUIController', operatorInstGUIController);

    operatorInstGUIController.$inject = ['$scope', 'dashboardService', 'eventService','spServicesService'];
    /* @ngInject */
    function operatorInstGUIController($scope, dashboardService, eventService, spServicesService) {
        var vm = this;
        //vm.picture[0] = "/images/blueBlock.png"
        //vm.picture[1] = "/images/greenBlock.png"
        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what
        vm.eventLog = [];
        vm.clearEventLog = clearEventLog;
        vm.listen = listen;
        vm.scope = $scope;
        vm.result = vm.position
        vm.imDone = imDone;
        activate();
        vm.position = ["blank", "blank", "blank", "blank", "blank", "blank", "blank", "blank"];


        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });

        }
        function imDone(done){
            vm.position = ["blank", "blank", "blank", "blank", "blank", "blank", "blank", "blank"];
            var mess = {"data": {"done": done}};

            spServicesService.callService(
                spServicesService.getService("operatorService"),mess,
                function(resp) {
                    if(_.has(resp, "attributes.result")){
                        vm.position = resp.attributes.result;
                    }
                })

        }
/*
        function imDone(done){
            var mess = {"data": {"done": done}};
            console.log("hej")
            console.log(spServicesService.getService("operatorService"));
            spServicesService.callService(
                spServicesService.getService("operatorService"),mess,
                function(resp) {
                    if(_.has(resp, "attributes.result")){
                        vm.result = resp.attributes.result;
                    }
                }
            )
        }
*/
        function listen(event){
            vm.eventLog.unshift(event);
            vm.scope.$apply();
        }

        function clearEventLog(){
            vm.eventLog = [];
        }


    }
})();
