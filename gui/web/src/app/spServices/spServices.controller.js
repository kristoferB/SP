/**
 * Created by patrik on 2015-09-14.
 */
(function () {
    'use strict';

    angular
      .module('app.spServices')
      .controller('spServicesController', spServicesController);

    spServicesController.$inject = ['spServicesService', '$scope', 'dashboardService','logger', 'modelService','itemService'];
    /* @ngInject */
    function spServicesController(spServicesService, $scope, dashboardService, logger, modelService,itemService) {
        var vm = this;
        var dashboard = $scope.$parent.$parent.$parent.vm.dashboard;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //For GUI
        vm.registeredServices = spServicesService.spServices; //From REST-api
        vm.displayedRegisteredServices = []; //For GUI
        vm.startSpService = startSPService; //To start a service. Name of service as parameter
        vm.currentProgess = {};
        vm.serviceAttributes = {};
        vm.isServiceActive = isServiceActive;

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });

            _.forEach(vm.registeredServices, function(s){
                vm.serviceAttributes[s.name] = {};
            });

        }

        function startSPService(spService) {
            spServicesService.callService(spService, {"data":vm.serviceAttributes[spService.name]}, resp, prog);

            if (!_.isUndefined(vm.currentProgess[event.service])){
                delete vm.currentProgess[event.service];
            }
        }

        function resp(event){
            console.log("RESP GOT: ");
            console.log(event);

            if (event.isa === 'Response') {
                for(var i = 0; i < event.ids.length; i++) {
                    if (!_.isUndefined(event.ids[i].sop)) {
                        var widgetKind = _.find(dashboardService.widgetKinds, {title: 'SOP Maker'});
                        var widgetStorage = {
                            sopSpec: event.ids[i]
                        };
                        dashboardService.addWidget(dashboard, widgetKind, widgetStorage);
                    }
                }
            }
            updateInfo(event);
        }

        function prog(event){
            console.log("PROG GOT: ");
            console.log(event);

            updateInfo(event);
        }

        function updateInfo(event){
            var error = "";
            if (!_.isUndefined(event.serviceError)){
                error = event.serviceError.error
            }
            var info = {
                service: event.service,
                reqID: event.reqID,
                info: event.attributes,
                error: error,
                type: event.isa,
                ids: event.ids
            }

            vm.currentProgess[event.service] = info;
        }

        function isServiceActive(name){
            if (!_.isUndefined(vm.currentProgess[name]))
                console.log("service: "+name+ " is active");

            return !_.isUndefined(vm.currentProgess[name])
        }

        function success(data) {

        }

    }
})();
