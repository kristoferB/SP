/**
 * Created by Daniel on 2016-04-29
 */
(function () {
    'use strict';

    angular
    .module('app.robotCycleAnalysis')
    .factory('robotCycleAnalysisService', robotCycleAnalysisService);

    robotCycleAnalysisService.$inject = ['eventService', 'spServicesService'];
    /* @ngInject */
    function robotCycleAnalysisService(eventService, spServicesService) {

        var service = {
            state: {
                busSettings: {
                    host: null,
                    port: null,
                    topic: null
                },
                busConnected: null,
                robotsListenedTo: []
            },
            connectToBus: connectToBus,
            disconnectFromBus: disconnectFromBus
        };

        var spServiceName = 'RobotCycleAnalysis';

        activate();

        return service;

        function activate() {
            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
            getServiceState();
        }

        function onEvent(ev){
            if (_.has(ev, "busConnected"))
                state.busConnected = ev.busConnected;
            if (_.has(ev, "busSettings"))
                state.busSettings = ev.busSettings;
            if (_.has(ev, "robotsListenedTo"))
                state.robotsListenedTo = ev.robotsListenedTo;
            if (_.has(ev, "robot")) {
                if (_.has(ev, "isListenedTo")) {
                    if (ev.isListenedTo)
                        state.robotsListenedTo.push(ev.robot);
                    else
                        state.robotsListenedTo = _(state.robotsListenedTo).filter(function(item) {
                            return item.name !== ev.robot.name;
                        });
                }
                if (_.has(ev, "routineStartOrStop")) {
                    
                }
            }

        }

        function getServiceState() {
            return postCommand("getServiceState").then(updateState);
        }

        function setupBus(host, port, topic) {
            var message = {
                command: "setupBus",
                busSettings: {
                    host: host,
                    port: port,
                    topic: topic
                }
            };
            return postToSP(message);
        }

        function connectToBus() {
            return postCommand("connectToBus")
        }

        function disconnectFromBus() {
            return postCommand("disconnectFromBus")
        }

        function postCommand(command) {
            var message = {
                command: command
            };
            return postToSP(message);
        }

        function postToSP(message) {
            message.core = {
                model: null,
                responseToModel: false,
                includeIDAbles: [],
                onlyResponse: true
            };
            return spServicesService.callService(spServiceName, message);
        }

        function updateState(spServiceState) {
            console.log("RobotCycleAnalysis: State was updated");
            service.state = spServiceState;
        }
    }

})();
