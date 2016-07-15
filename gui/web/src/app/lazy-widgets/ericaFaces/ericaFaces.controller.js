/**
 * Created by Edvard on 2016-06-15.
 */
(function () {
    'use strict';

    angular
      .module('app.ericaFaces')
      .controller('ericaFacesController', ericaFacesController);

    ericaFacesController.$inject = ['$scope', 'dashboardService', 'eventService'];
    /* @ngInject */
    function ericaFacesController($scope, dashboardService, eventService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what

        activate();

        function activate() {
            $scope.$on('closeRequest', function () {
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
        }

        function onEvent() {
            //console.log("It has to be done");
            //reset();
        }

    }
})();
