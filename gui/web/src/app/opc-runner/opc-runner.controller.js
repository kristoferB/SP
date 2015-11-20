/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.opcRunner')
      .controller('opcRunnerController', opcRunnerController);

    opcRunnerController.$inject = ['opcRunnerService', '$scope', 'dashboardService','logger', 'modelService','itemService'];
    /* @ngInject */
    function opcRunnerController(opcRunnerService, $scope, dashboardService, logger, modelService,itemService) {
        var vm = this;
        vm.x = 2;
        vm.opcServ = opcRunnerService;
        vm.test = function() {
            vm.x = 5;
        }
        activate();

        function activate() {
            vm.x = 4;
        }

        function blah() {
            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                addEventListener(id, responseCallBack, progressCallback);
                message.reqID = id;
                message.data = { 
                    "test" : 2
                };
                return restService.postToServiceInstance(message, "opc")
            });

            return answerF.then(function(serviceAnswer){
                //logger.info('service answer: ' + JSON.stringify(serviceAnswer) + '.');
                return serviceAnswer;
            })

            spServicesService.callService(spService, {"data":vm.serviceAttributes[spService.name]}, resp, prog);
        }

        function resp(event){
            console.log("RESP GOT: ");
            console.log(event);
        }

        function prog(event){
            console.log("PROG GOT: ");
            console.log(event);
        }        

        function startSPService(spService) {

            //Fill attributes with default values if spService directive has not been loaded.
            if(_.isUndefined(vm.serviceAttributes[spService.name])) {
                vm.serviceAttributes[spService.name] = fillAttributes(spService.attributes,"");
//                console.log("vm.serviceAttributes[spService.name] " + JSON.stringify(vm.serviceAttributes[spService.name]));
            }

            

            if (!_.isUndefined(vm.currentProgess[event.service])){
                delete vm.currentProgess[event.service];
            }
        }

        function fillAttributes(structure,key) {
            var x = structure;
            if (_.isUndefined(x)){
                //Do nothing
            } else if (!_.isUndefined(x.ofType)){
                //core>model
                if (x.ofType == "Option[ID]" && key == "model") {
                    console.log("inside" + x.default);
                    return _.isUndefined(x.default) ? spServicesService.reloadModelID() : x.default;
                //core>includeIDAbles
                } else if (x.ofType == "List[ID]" && key == "includeIDAbles") {
//                    return _.isUndefined(x.default) ? spServicesService.reloadSelectedItems() : x.default;
                return spServicesService.reloadSelectedItems();
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
