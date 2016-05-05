/**
 * Created by Kristofer on 2016-02-20.
 */
(function () {
    'use strict';

    angular
        .module('app.operatorInstGUI')
        .factory('operatorInstGUIService', operatorInstGUIService);

    operatorInstGUIService.$inject = ['$q','logger', 'eventService', 'spServicesService', 'restService'];
    /* @ngInject */
    function operatorInstGUIService($q, logger, eventService, spServicesService, restService) {
        var service = {
            connect: connect,
            connected: false,
            serviceID: '',
            done: done,
            operatorInstructions: {}
        };

        const serviceName = 'OperatorInstructions';

        activate();

        return service;

        function activate() {
            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
        }

        function onEvent(ev){
            console.log('operator instructions event...');

            if (!_.has(ev, 'reqID') || ev.reqID !== service.serviceID) return;

            if (_.has(ev, 'attributes.theBus')){
                service.connected = ev.attributes.theBus === 'Connected';
                console.log('connected = ' + service.connected);
            }

            if (_.has(ev, 'attributes.operatorInstructions')){
                service.operatorInstructions = ev.attributes.operatorInstructions;
            }            
        }

        function connect(busIP,publishTopic,subscribeTopic){
            var mess = {
                'setup': {
                    'busIP': busIP,
                    'publishTopic': publishTopic,
                    'subscribeTopic': subscribeTopic
                }
            };
            spServicesService.callService(serviceName,{'data':mess}).then(function(repl){
                console.log("inside first connection");
                console.log(repl);
                if (messageOK(repl) && _.has(repl, 'reqID')){
                    service.serviceID = repl.reqID;
                }
            });
        }
        
        function done() {
            var mess = {
                'command': 'done'
            };
            spServicesService.callService(serviceName,{'data':mess});
            service.operatorInstructions = {};
        }

        function messageOK(mess){
            var msg = restService.errorToString(event);
            if (msg !== ""){
                logger.error(msg);
                return false;
            } else return true;
        }
    }

})();
