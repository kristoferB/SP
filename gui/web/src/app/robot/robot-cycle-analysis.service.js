/**
 * Created by Daniel on 2016-04-29
 */
(function () {
    'use strict';

    angular
    .module('app.operationControl')
    .factory('robotCycleAnalysisService', robotCycleAnalysisService);

    robotCycleAnalysisService.$inject = ['$q','logger', 'eventService', 'spServicesService', 'restService'];
    /* @ngInject */
    function robotCycleAnalysisService($q, logger, eventService, spServicesService, restService) {

        var service = {
            state: {
                bus: {
                    host: null,
                    port: null,
                    topic: null,
                    connected: false
                }
            },
            connect: connect,
            disconnect: disconnect,
            subscribe: subscribe,
            unsubscribe: unsubscribe
        };

        var spServiceId = 'RobotCycleAnalysis';

        activate();

        return service;

        function activate() {
            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
            getInitialState();
        }

        function onEvent(ev){
            
        }

        function getInitialState() {
            return postCommand("getInitialState").then(updateState);
        }

        function connect() {
            return postCommand("connect")
        }

        function disconnect() {
            return postCommand("disconnect")
        }

        function subscribe() {
            return postCommand("subscribe")
        }

        function unsubscribe() {
            return postCommand("unsubscribe")
        }

        function postCommand(command) {
            var message = {
                command: {
                    kind: command
                }
            };
            return postToSP(message);
        }

        function postToSP(message) {
            message["core"] = {
                model: null,
                responseToModel: false,
                includeIDAbles: [],
                onlyResponse: true
            };
            return restService.postToServiceInstance(message, spServiceId);
        }

        function updateState(spServiceState) {
            service.state = spServiceState;
        }
    }

})();
