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
        vm.done = false
        activate();
        vm.palett = ["#fff","#fff","#fff","#fff","#fff","#fff","#fff","#fff"];


        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
            //Some problem with the listner here
            eventService.addListener('Response',dummyFunction())
            function dummyFunction(data){
                console.log("Listener ")
            }
        }
        function imDone(done){
            var dummyArray = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0];
            var mess = {"data": {"getNext": done,"buildOrder": dummyArray}};

            spServicesService.callService(
                spServicesService.getService("operatorService"),mess,
                function(resp) {
                    if(_.has(resp, "attributes.result")){
                        vm.result = resp.attributes.result;
                        console.log("Hej" + vm.result[8]);
                        /*
                        Check if response is completly blank, if blank set variable true and update palett
                        Otherwise just update palett
                        Create a listener and a function that when the variable is true and an event happens,
                        set variable to false and update palett.
                        Variable Should be init to true
                        */
                    }
                })

        }
        function listen(event){
            vm.eventLog.unshift(event);
            vm.scope.$apply();
        }

        function clearEventLog(){
            vm.eventLog = [];
        }


    }
})();
