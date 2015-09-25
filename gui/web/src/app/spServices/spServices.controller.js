/**
 * Created by patrik on 2015-09-14.
 */
(function () {
    'use strict';

    angular
      .module('app.spServices')
      .controller('spServicesController', spServicesController);

    spServicesController.$inject = ['spServicesService', '$scope', 'dashboardService','logger'];
    /* @ngInject */
    function spServicesController(spServicesService, $scope, dashboardService, logger) {
        var vm = this;
        var dashboard = $scope.$parent.$parent.$parent.vm.dashboard;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //For GUI
        vm.registeredServices = spServicesService.spServices; //From REST-api
        vm.displayedRegisteredServices = []; //For GUI
        vm.startSpService = startSPService; //To start a service. Name of service as parameter
        vm.currentProgess = {};
        vm.isServiceActive = isServiceActive;
        vm.preProcessServiceAttr = preProcessServiceAttr;
        //<---Temp fields--->
        vm.printToLogger = function(obj) {logger.info("I was asked to print this: "+obj);}
        vm.serviceForms = {};
        //<----------------->

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }

        function startSPService(spService) {
            logger.info("service form: " + spService.name + " " + JSON.stringify(vm.serviceForms[spService.name]));
//            spServicesService.callService(spService, {"data":{"setup":{"sopname":"some name"}}}, resp, prog);
            spServicesService.callService(spService, {}, resp, prog);

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

        function preProcessServiceAttr(serviceAsJson) {
            vm.serviceForms[serviceAsJson.name] = buildServiceFormFromJsonAttr(serviceAsJson);
            return buildHtmlFromJsonAttr(serviceAsJson);
        }

        function buildHtmlFromJsonAttr(serviceAsJson) {
            var toReturn = '';
            function recBuild(obj, parentString) {
                var k;
                if (obj instanceof Object) {
                    var keys = [];
                    for (k in obj) {
                        keys.push(k);
                    }
                    //Test for obj(ect) to be a KeyDefinition
                    //From the signature of a KD it will always contains keys 'domain' and 'ofType'.
                    if (_.contains(keys,"domain") && _.contains(keys,"ofType")) {
                        //Yes, this obj(ect) is a KeyDefinition
                        toReturn += '<i>KeyDefinition of type:' + obj["ofType"] + '</i>';
                    } else {
                        //No, this obj(ect) is not a KeyDefinition
                        for (k in obj){
                            if (obj.hasOwnProperty(k)){
                                toReturn += '<b>' + k + '</b>';
                                var value = obj[k];
                                var updatedParentString = parentString + "." + k;
                                if (value instanceof Object) {
                                    toReturn += '<ul>';
                                    recBuild(obj[k], updatedParentString);
                                    toReturn += '</ul>';
                                } else {
                                    toReturn += ': <input type="text" ng-model="'+updatedParentString+'"/>' + value + '<br/>';
                                }
                            }
                        }
                    }
                } else {
                    toReturn += '<li><button ng-click="vm.printToLogger(\''+obj+'\')"> :(' + obj+ '</button></li>';
                };
            };

            recBuild(serviceAsJson.attributes, "vm.serviceForms." + serviceAsJson.name);
            return toReturn;
        };

        function buildServiceFormFromJsonAttr(serviceAsJson) {
            function recBuild(obj) {
                var k;
                if (obj instanceof Object) {
                    var objToReturn = {};
//                    var keys = [];
//                    for (k in obj) {
//                        keys.push(k);
//                    }
//                    //Test for obj(ect) to be a KeyDefinition
//                    //From the signature of a KD it will always contains keys 'domain' and 'ofType'.
//                    if (_.contains(keys,"domain") && _.contains(keys,"ofType")) {
//                        //Yes, this obj(ect) is a KeyDefinition
//                        return 'KeyDefinition';
//                    } else {
                        //No, this obj(ect) is not a KeyDefinition
                        for (k in obj){
                            if (obj.hasOwnProperty(k)){
                                var fromProperty = recBuild(obj[k])
                                objToReturn[k] = fromProperty;
                            }
                        }
                        return objToReturn;

//                    }
                } else {
                    return obj;
                };
            };
            return recBuild(serviceAsJson.attributes);
        };


    }
})();
