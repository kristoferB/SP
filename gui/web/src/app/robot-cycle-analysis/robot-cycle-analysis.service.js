/**
 * Created by Daniel on 2016-04-29
 */
(function () {
    'use strict';

    angular
    .module('app.robotCycleAnalysis')
    .factory('robotCycleAnalysisService', robotCycleAnalysisService);

    robotCycleAnalysisService.$inject = ['eventService', 'spServicesService', 'logger'];
    /* @ngInject */
    function robotCycleAnalysisService(eventService, spServicesService, logger) {

        var service = {
            state: {
                availableWorkCells: [],
                busSettings: {
                    host: null,
                    port: null,
                    topic: null
                },
                busConnected: null,
                connectionInterrupted: null,
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
            eventService.addListener('Progress', onEvent);
            eventService.addListener('ServiceError', onError);
            getServiceState();
        }

        function onError(ev) {
            if (_.has(ev.serviceError, "error"))
                logger.error(ev.serviceError.error)
        }

        function onEvent(ev) {
            console.log('Robot cycle analysis service received response or progress event ', ev);
            var attrs = ev.attributes;
            if (_.has(attrs, 'busConnected'))
                service.state.busConnected = attrs.busConnected;
            if (_.has(attrs, 'connectionInterrupted'))
                service.state.connectionInterrupted = attrs.connectionInterrupted;
            if (_.has(attrs, 'busSettings'))
                service.state.busSettings = attrs.busSettings;
            if (_.has(attrs, 'availableWorkCells'))
                service.state.availableWorkCells = attrs.availableWorkCells;
            if (_.has(attrs, 'addedLiveWatch'))
                service.state.liveWorkCells.push(attrs.addedLiveWatch);
            if (_.has(attrs, 'removedLiveWatch'))
                _.filter(service.state.liveWorkCells, function (liveWorkCell) {
                    return liveWorkCell.name !== attrs.removedLiveWatch.name;
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
            _.merge(service.state, spServiceState.attributes);
        }
    }

})();
