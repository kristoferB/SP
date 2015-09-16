/**
 * Created by patrik on 2015-09-14.
 */
(function () {
    'use strict';

    angular
        .module('app.spServices')
        .factory('spServicesService', spServicesService);

    spServicesService.$inject = ['$q', 'logger', 'restService'];
    /* @ngInject */
    function spServicesService($q, logger, restService) {
        var service = {
            spServices: []
        };

        activate();

        return service;

        function activate() {
            var promises = [getRegisteredSpServices()];
            return $q.all(promises).then(function() {
                logger.info('spServices service: Loaded ' + service.spServices + ' spServices through REST.');
            });
        }

        function getRegisteredSpServices() {
            restService.getRegisteredServices().then(function (data) {
//                logger.info("service" + JSON.stringify(data))
                service.spServices.push.apply(service.spServices, data);
            });
        }

    }
})();
