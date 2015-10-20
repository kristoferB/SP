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
        vm.showDetails = {};
        vm.openResponse = {};
        vm.isServiceActive = isServiceActive;
        vm.servicesAreRunnable = servicesAreRunnable;

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });

            _.forEach(vm.registeredServices, function(s){
                vm.showDetails[s.name] = false;
                vm.openResponse[s.name] = false;
            });

        }

        function startSPService(spService) {

            //Fill attributes with default values if spService directive has not been loaded.
            if(_.isUndefined(vm.serviceAttributes[spService.name])) {
                vm.serviceAttributes[spService.name] = fillAttributes(spService.attributes,"");
            }

            spServicesService.callService(spService, {"data":vm.serviceAttributes[spService.name]}, resp, prog);

            if (!_.isUndefined(vm.currentProgess[event.service])){
                delete vm.currentProgess[event.service];
            }
        }

        function resp(event){
            console.log("RESP GOT: ");
            console.log(event);

//            if (event.isa === 'Response') {
//                for(var i = 0; i < event.ids.length; i++) {
//                    if (!_.isUndefined(event.ids[i].sop)) {
//                        var widgetKind = _.find(dashboardService.widgetKinds, {title: 'SOP Maker'});
//                        var widgetStorage = {
//                            sopSpec: event.ids[i]
//                        };
//                        dashboardService.addWidget(dashboard, widgetKind, widgetStorage);
//                    }
//                }
//            }
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

        function servicesAreRunnable() {
            return !(modelService.activeModel === null)
        }

        function fillAttributes(structure,key) {
            var x = structure;
            if (_.isUndefined(x)){
                //Do nothing
            } else if (!_.isUndefined(x.ofType)){
                //core>model
                if (x.ofType == "Option[ID]" && key == "model") {
                    return _.isUndefined(x.default) ? spServicesService.reloadModelID() : x.default;
                //core>includeIDAbles
                } else if (x.ofType == "List[ID]" && key == "includeIDAbles") {
                    return _.isUndefined(x.default) ? spServicesService.reloadSelectedItems() : x.default;
                //Boolean
                } else if (x.ofType == "Boolean") {
                    return _.isUndefined(x.default) ? false : x.default;
                //String
                } else if (x.ofType == "String") {
                    return _.isUndefined(x.default) ? "" : x.default;
                //Int
                } else if (x.ofType == "Int") {
                    return _.isUndefined(x.default) ? 0 : x.default;
                //List[ID] and List[String]
                } else if (x.ofType == "List[ID]" || x.ofType == "List[String]") {
                    return _.isUndefined(x.default) ? [] : x.default;
                //The rest
                } else {
                    return _.isUndefined(x.default) ? "" : x.default;
                }
            } else if (_.isObject(x)){
                var localAttribute = {};
                for(var localKey in x){
                    var attrName = localKey;
                    var attrValue = x[localKey];
                    localAttribute[attrName] = fillAttributes(attrValue,attrName);
                }
                return localAttribute;
            } else {
                return x;
            }
        }

    }
})();
