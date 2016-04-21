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
        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what
        vm.eventLog = [];
        vm.clearEventLog = clearEventLog;
        vm.listen = listen;
        vm.scope = $scope;
        vm.result = vm.position
        vm.imDone = imDone;
        vm.done = false
        vm.message = "Hej"



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
            //Some problem with the listner here
            eventService.addListener('Response',vm.listen);
        }
        function imDone(done){

            var mess = {"data": {"getNext": done,"buildOrder": vm.Palett.sendEmpty}};

            spServicesService.callService(
                spServicesService.getService("operatorService"),mess,
                function(resp) {
                    if(_.has(resp, "attributes.result")){
                        vm.Palett.pal = resp.attributes.result;

                    }
                })

        }
        function listen(event){
            vm.eventLog.unshift(event);
            vm.scope.$apply();
            if (event.service == "operatorService"){
                vm.Palett.pal = event.attributes.result;
                vm.message = resp.attributes.hej;
            }
        }

        function clearEventLog(){
            vm.eventLog = [];
        }


    }
})();
