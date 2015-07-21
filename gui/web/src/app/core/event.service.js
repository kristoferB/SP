(function () {
    'use strict';

    angular
        .module('app.core')
        .factory('event', event);

    event.$inject = ['$rootScope', 'API', 'logger'];
    /* @ngInject */
    function event($rootScope, API, logger) {
        var eventSource = null;
        var service = {
            addListener: addListener
        };

        activate();

        return service;

        function activate() {
            openSSEChannel();
        }

        function openSSEChannel() {
            eventSource = new EventSource(API.events);
            $rootScope.$on('$destroy', function() {
                eventSource.close();
            });
        }

        function addListener(target, handlerFunc) {
            if(eventSource === null) {
                logger.error('Couldn\'t add an SSE listener for target ' + target + ' because there\'s no ' +
                    'EventSource to add it to.');
            } else {
                eventSource.addEventListener(target, function(e) {
                    const data = JSON.parse(e.data);
                    logger.info('Received ' + data.event + ' event for target ' + target + '.');
                    $rootScope.$apply(handlerFunc(data));
                });
                logger.info('Added a SSE listener for target ' + target + '.');
            }

        }

    }
})();
