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
            busSettings: {
                host: null,
                port: null,
                topic: null
            },
            busConnected: null,
            connectToBus: connectToBus,
            disconnectFromBus: disconnectFromBus,
            isInterrupted: null,
            publishWorkCellListOpenedEvent: publishWorkCellListOpenedEvent,
            searchCycles: searchCycles,
            setupBus: setupBus,
            startLiveWatch: startLiveWatch,
            stopLiveWatch: stopLiveWatch
        };

        var spServiceName = 'RobotCycleAnalysis';

        activate();

        return service;

        function activate() {
            eventService.addListener('Response', onResponse);
            eventService.addListener('ServiceError', onError);
            getServiceState();
        }

        function onError(ev) {
            if (ev.service == spServiceName && _.has(ev.serviceError, 'error'))
                logger.error(ev.serviceError.error);
        }

        function onResponse(ev) {
            var attrs = ev.attributes;
            if (_.has(attrs, 'busConnected'))
                service.busConnected = attrs.busConnected;
            if (_.has(attrs, 'isInterrupted'))
                service.isInterrupted = attrs.isInterrupted;
            if (_.has(attrs, 'busSettings'))
                service.busSettings = attrs.busSettings;
            if (_.has(attrs, 'addedLiveWatch'))
                service.liveWorkCells.push(attrs.addedLiveWatch);
            if (_.has(attrs, 'removedLiveWatch'))
                _.filter(service.liveWorkCells, function (liveWorkCell) {
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
            return postCommand('connectToBus');
        }

        function disconnectFromBus() {
            return postCommand('disconnectFromBus');
        }

        function searchCycles(message) {
            message['command'] = 'searchCycles';
            return postToSP(message);
        }

        function startLiveWatch(workCellId) {
            var message = {
                command: 'startLiveWatch',
                workCellId: workCellId
            };
            return postToSP(message);
        }

        function stopLiveWatch(workCellId) {
            var message = {
                command: 'stopLiveWatch',
                workCellId: workCellId
            };
            return postToSP(message);
        }

        function publishWorkCellListOpenedEvent() {
            return postCommand('publishWorkCellListOpenedEvent');
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
            _.merge(service, spServiceState.attributes);
        }
    }

})();
