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
        var dashboard = $scope.$parent.$parent.$parent.vm.dashboard;
        vm.widget = $scope.$parent.$parent.$parent.widget; //For GUI
        vm.registeredServices = spServicesService.spServices; //From REST-api
        vm.displayedRegisteredServices = []; //For GUI
        vm.startSpService = startSPService; //To start a service. Name of service as parameter

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }

        function startSPService(spService) {
            spServicesService.startSpService(spService).then(success);

            function success(data) {
                if (data.isa === 'Response') {
                    for(var i = 0; i < data.ids.length; i++) {
                        var widgetKind = _.find(dashboardService.widgetKinds, {title: 'SOP Maker'});
                        var widgetStorage = {
                            sopSpec: data.ids[i]
                        };
                        dashboardService.addWidget(dashboard, widgetKind, widgetStorage);
                    }
                }
            }
        }

    }
})();
