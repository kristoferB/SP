(function () {
    'use strict';

    angular
        .module('app.core')
        .factory('eventHandler', eventHandler);

    eventHandler.$inject = ['$rootScope', 'API', 'logger'];
    /* @ngInject */
    function eventHandler($rootScope, API, logger) {
        var eventSource = new EventSource(API.events);
        var service = {
            addListener: addListener,
            eventSource: eventSource
        };

        activate();

        return service;

        function activate() {
            openSSEChannel();
        }

        /* global EventSource */
        function openSSEChannel() {
            if (typeof(EventSource) !== 'undefined') {
                $rootScope.$on('$destroy', function () {
                    eventSource.close();
                });
            } else {
                logger.error('Your browser does\'nt support SSE. Please update your browser.');
            }
        }

        function addListener(target, handlerFunc) {
            if (eventSource === null) {
                logger.error('Couldn\'t add an SSE listener for target ' + target + ' because there\'s no ' +
                    'EventSource to add it to.');
            } else {
                eventSource.addEventListener(target, function(e) {
                    var data = angular.fromJson(e.data);
                    logger.info('Received ' + data.event + ' event for target ' + target + '.');
                    $rootScope.$apply(handlerFunc(data));
                });
                logger.info('Added a SSE listener for target ' + target + '.');
            }

        }

    }
})();
