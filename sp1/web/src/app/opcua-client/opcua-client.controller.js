/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.opcuaClient')
      .controller('opcuaClientController', opcuaClientController);

    opcuaClientController.$inject = ['opcuaClientService', '$scope', 'dashboardService','logger', 'modelService','itemService','spServicesService','restService','eventService','$interval'];
    /* @ngInject */
    function opcuaClientController(opcuaClientService, $scope, dashboardService, logger, modelService,itemService,spServicesService,restService,eventService,$interval) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what

        vm.url = 'opc.tcp://localhost:12686';
        vm.connected = false;
        vm.nodes = [];
        vm.selected = [];

        vm.connect = connect;
        vm.getNodes = getNodes;
        vm.subscribe = subscribe;
        vm.write = write;

        vm.toggleSubscription = function(item) {
            var idx = vm.selected.indexOf(item);
            if (idx > -1) {
                vm.selected.splice(idx, 1);
            }
            else {
                vm.selected.push(item);
            }
        };

        vm.isSubscriptionToggled = function(item) {
            return vm.selected.indexOf(item) > -1;
        };

        function onEvent(event){
            // only care about OPC service, assume simulation service always finishes in time
            if(!(_.isUndefined(event.service)) && event.service != "OpcUARuntime") return;
            if(!(_.isUndefined(event.isa)) && event.isa != "Response") return;

            if(_.has(event, 'attributes.connected')) {
                vm.connected = event.attributes.connected;
            }

            if(_.has(event, 'attributes.state')) {
                _.forEach(event.attributes.state, function(v,k) {
                    var idx = _.findIndex(vm.nodes, function(o) { return o.name == k; });
                    if(idx > -1) {
                        console.log(idx);
                        vm.nodes[idx].value = v;
                    }
                });
            }

            console.log(event);
        }

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });

            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
        }

        function messageOK(message){
            var err = restService.errorToString(message);
            if(err !== "") {
                logger.error(err);
                return false;
            } else return true;
        } 

        function connect() {
            var message = {
                "cmd": "connect",
                "address": vm.address
            };
            var f = spServicesService.callService('OpcUARuntime',{'data':message}, function(repl) {
                if (messageOK(repl) && _.has(repl, 'attributes.connected')) {
                    vm.connected = repl.attributes.connected;
                }}, function(progress) {});
        }

        function getNodes() {
            var message = {
                "cmd": "getNodes"
            };
            var f = spServicesService.callService('OpcUARuntime',{'data':message}, function(repl) {
                if (messageOK(repl) && _.has(repl, 'attributes.nodes')) {
                    vm.nodes = _.map(repl.attributes.nodes, function(v,k) { return { 'name':k, 'datatype':v, 'value':0 };});
                }}, function(progress) {});
        }

        function subscribe() {
            var message = {
                "cmd": "subscribe",
                "nodes": vm.selected
            };
            var f = spServicesService.callService('OpcUARuntime',{'data':message}, function(repl) {}, function(progress) {});
        }

        function write(item) {
            var value = false;
            if(item.datatype == 'Boolean') {
                if(item.value == '1' || item.value == 'true')
                    value = true;
            }
            else if(item.datatype == 'UByte') {
                value = 1.0 * item.value;
            }
            var message = {
                "cmd": "write",
                "node": item.name,
                "value": value
            };
            
            var f = spServicesService.callService('OpcUARuntime',{'data':message}, function(repl) {}, function(progress) {});            
        }        

        activate();
    }
})();
