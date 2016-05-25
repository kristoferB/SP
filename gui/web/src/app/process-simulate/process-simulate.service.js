/**
 * Created by Martin on 2016-05-24.
 */
(function () {
    'use strict';

    angular
        .module('app.processSimulate')
        .factory('processSimulateService', processSimulateService);

    processSimulateService.$inject = ['$q','logger', 'eventService', 'spServicesService', 'restService', 'itemService'];
    /* @ngInject */
    function processSimulateService($q, logger, eventService, spServicesService, restService, itemService) {
        var service = {
            connected: false,
            serviceID: '',
            connect: connect
        };

        activate();

        return service;

        function activate() {
            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
        }

        function onEvent(ev){
            if (!_.has(ev, 'reqID') || ev.reqID !== service.serviceID) return;

            if (_.has(ev, 'attributes.bus')){
                service.connected = ev.attributes.bus === 'Connected';
            }
        }

        function connect(ip, topic){
            var mess = {
                'setup': {
                    'ip': 'localhost',
                    'topic': 'ps'
                },
                'command':{
                    'type':'connect'
                }
            };
            spServicesService.callService('ProcessSimulate',{'data':mess}).then(function(repl){
                if (mok(repl) && _.has(repl, 'reqID')){
                    service.serviceID = repl.reqID;
                }
            });
        }

        function mok(m) {
            return restService.errorToString(m) === "";
        }
    }
})();
