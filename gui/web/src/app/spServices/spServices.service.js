/**
 * Created by patrik on 2015-09-14.
 */
(function () {
    'use strict';

    angular
        .module('app.spServices')
        .factory('spServicesService', spServicesService);

    spServicesService.$inject = ['$q', 'logger', 'restService', '$http'];
    /* @ngInject */
    function spServicesService($q, logger, restService, $http) {
        var service = {
            spServices1: getSpServices,
            spServices2: []
        };

        activate();

        return service;

        function activate() {
            var promises = [getRegisteredSpServices()];
            return $q.all(promises).then(function() {
                logger.info('spServices service: Loaded ' + service.spServices2 + ' spServices through REST.');
            });
        }

        function getRegisteredSpServices() {
            return restService.getRegisteredServices().then(function (data) {
                service.spServices2.push.apply(service.spServices2, data.list);
            });
        }

        function getSpServices() {
            logger.info('Trying to get spServices');

            restService.getRegisteredServices().then(function(dataFromServer)  {
                logger.success('success to get spServices');
                return dataFromServer.list;
            });

//            $http.get('api/services').success(function(dataFromServer)  {
//                logger.success('success to get spServices');
//                return dataFromServer.list;
//            }).error(function(dataFromServer) {
//                logger.info('problem to get  spServices');
//            });
        }

    }
})();
