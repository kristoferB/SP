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
                availableWorkCells: [],
                busSettings: {
                    host: null,
                    port: null,
                    topic: null
                },
                busConnected: null,
                liveWorkCells: null
            },
            setupBus: setupBus,
            connectToBus: connectToBus,
            disconnectFromBus: disconnectFromBus
        };

        var spServiceName = 'RobotCycleAnalysis';

        activate();

        return service;

        function activate() {
            eventService.addListener('Response', onEvent);
            getServiceState();
        }

        function onEvent(ev){
            console.log("Robot cycle analysis service received ", ev);
            if (_.has(ev, 'busConnected'))
                service.state.busConnected = ev.busConnected;
            if (_.has(ev, 'busSettings')) {
                console.log("Bus settings received in robot cycle analysis service.");
                service.state.busSettings = ev.busSettings;
            }
            if (_.has(ev, 'availableWorkCells'))
                service.state.availableWorkCells = ev.availableWorkCells;
            if (_.has(ev, 'addedLiveWatch'))
                service.state.liveWorkCells.push(ev.addedLiveWatch);
            if (_.has(ev, 'removedLiveWatch'))
                _.filter(service.state.liveWorkCells, function (liveWorkCell) {
                    return liveWorkCell.name !== ev.removedLiveWatch.name;
                });
        }

        function getServiceState() {
            return postCommand("getServiceState").then(updateState);
        }

        function setupBus(busSettings) {
            var message = {
                command: "setupBus",
                busSettings: busSettings
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
            console.log("RobotCycleAnalysis: Service state was updated");
            service.state.availableWorkCells = spServiceState.availableWorkCells;
            service.state.busSettings = spServiceState.busSettings;
            service.state.busConnected = spServiceState.busConnected;
            service.state.liveWorkCells = spServiceState.liveWorkCells;
        }
    }

})();
