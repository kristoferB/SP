/**
 * Created by patrik on 2015-09-14.
 */
(function () {
    'use strict';

    angular
        .module('app.spServices')
        .controller('spServicesController', spServicesController);

    spServicesController.$inject = ['spServicesService', '$scope', 'dashboardService'];
    /* @ngInject */
    function spServicesController(spServicesService, $scope, dashboardService) {
        var vm = this;
        vm.widget = $scope.$parent.widget; //For GUI
        vm.registeredServices = spServicesService.spServices; //From RESTapi
        vm.displayedRegisteredServices = []; //For GUI
        vm.startSpService = spServicesService.startSpService; //To start a service. Name of service as parameter

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }

    }
})();
