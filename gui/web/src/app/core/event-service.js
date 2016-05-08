(function () {
    'use strict';

    angular
        .module('app.core')
        .factory('eventService', eventService);

    eventService.$inject = ['$rootScope', 'API', 'logger'];
    /* @ngInject */
    function eventService($rootScope, API, logger) {
        var service = {
            addListener: addListener,
            eventSource: null,
            removeListener: removeListener
        };

        return service;



        /* global EventSource */
        function createEventSource() {
            if (typeof(EventSource) !== 'undefined') {
                service.eventSource = new EventSource(API.events);
                $rootScope.$on('$destroy', function () {
                    service.eventSource.close();
                });
            } else {
                logger.error('Event Service: Your browser does\'nt support SSE. Please update your browser.');
            }
        }

        function addListener(target, handlerFunc) {
            if (service.eventSource === null) {
                createEventSource();
                /*logger.error('Couldn\'t add an SSE listener for target ' + target + ' because there\'s no ' +
                    'EventSource to add it to.');*/
            }
            service.eventSource.addEventListener(target, function(e) {
                var data = angular.fromJson(e.data);
                $rootScope.$apply(handlerFunc(data));
            });
            logger.info('Event Service: Added an SSE listener for target ' + target + '.');
        }

        function removeListener(eventType, listener) {
            service.eventSource.removeEventListener(eventType, listener);
        }

    }
})();
