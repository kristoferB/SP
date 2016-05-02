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
                availableCycles: [],
                availableWorkCells: [],
                busSettings: {
                    host: null,
                    port: null,
                    topic: null
                },
                busConnected: null,
                chosenWorkCell: null,
                historicalCycles: [],
                liveCycle: null,
                liveWorkCell: null
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
            if (_.has(ev, 'busConnected'))
                service.state.busConnected = ev.busConnected;
            if (_.has(ev, 'busSettings'))
                service.state.busSettings = ev.busSettings;
            if (_.has(ev, 'availableWorkCells'))
                service.state.availableWorkCells = ev.availableWorkCells;
            if (_.has(ev, 'availableCycles'))
                service.state.availableCycles = ev.availableCycles;
            if (_.has(ev, 'cycle')) {
                if (ev.cycle.id == "current")
                    service.state.liveCycle = ev.cycle;
                else
                    service.state.historicalCycles.push(ev.cycle);
            }
            if (_.has(ev, 'routineEvent')) {
                if (service.state.liveCycle !== null)
                    service.state.liveCycle.routineEvents.push(ev.routineEvent);
            }
            if (_.has(ev, 'liveWorkCell')) {
                state.liveWorkCell = ev.liveWorkCell;
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
