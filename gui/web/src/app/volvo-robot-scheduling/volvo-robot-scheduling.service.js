/**
 * Created by Martin on 2016-05-24.
 */
(function () {
    'use strict';

    angular
        .module('app.volvoRobotScheduling')
        .factory('volvoRobotSchedulingService', volvoRobotSchedulingService);

    volvoRobotSchedulingService.$inject = ['$q','logger', 'eventService', 'spServicesService', 'restService', 'itemService'];
    /* @ngInject */
    function volvoRobotSchedulingService($q, logger, eventService, spServicesService, restService, itemService) {
        var service = {
            connected: false,
            serviceID: '',
            connect: connect,
            disconnect: disconnect,
            command: command,
            roots: []
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

            if (_.has(ev, 'attributes.roots')){
                service.roots = ev.attributes.roots;
            }
        }

        function connect(ip, topic){
            var mess = {
                'setup': {
                    'ip': ip,
                    'topic': topic
                },
                'command':{
                    'type':'connect'
                }
            };
            command(mess);
        }

        function disconnect(){
            var mess = {
                'command':{
                    'type':'disconnect'
                }
            };
            command(mess);
            service.connected = false;
            service.serviceID = '';
            service.roots = [];
        }

        function command(message) {
            spServicesService.callService('ProcessSimulate',{'data':message}).then(function(repl){
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
