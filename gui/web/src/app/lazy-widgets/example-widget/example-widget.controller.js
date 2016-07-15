/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.exampleWidget')
      .controller('exampleWidgetController', exampleWidgetController);

    exampleWidgetController.$inject = ['$scope', 'dashboardService', 'eventService'];
    /* @ngInject */
    function exampleWidgetController($scope, dashboardService, eventService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what

        vm.eventLog = [];
        vm.clearEventLog = clearEventLog;
        vm.listen = listen;
        vm.scope = $scope;

        activate();


        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });

            eventService.addListener('ServiceError', vm.listen);
            eventService.addListener('SPError', vm.listen);
            eventService.addListener('Progress', vm.listen);
            eventService.addListener('Response', vm.listen);
            eventService.addListener('ModelDiff', vm.listen);
            eventService.addListener('ServiceEvent', vm.listen);
            eventService.addListener('ModelInfo', vm.listen);
            eventService.addListener('RemoveService', vm.listen);
            eventService.addListener('ServiceInfo', vm.listen);
            eventService.addListener('ServiceInfo', vm.listen);

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
