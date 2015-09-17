/**
 * Created by patrik on 2015-09-14.
 */
(function () {
    'use strict';

    angular
        .module('app.spServices')
        .factory('spServicesService', spServicesService);

    spServicesService.$inject = ['$q', 'logger', 'restService', 'modelService', 'itemService'];
    /* @ngInject */
    function spServicesService($q, logger, restService, modelService, itemService) {
        var service = {
            spServices: [],
            startSpService: startSpService
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

        function startSpService(spService) {
//            logger.info("sp services - service: Started service " + spService.name)
            var attributesSentToService = {'activeModel': modelService.activeModel, 'selectedItems': itemService.selected}
            var responseF = restService.postToServiceInstance(attributesSentToService, spService.name)

              responseF.then(function(data){
                logger.info('service answer: ' + JSON.stringify(data) + '.');
            })


            ;
        }

    }
})();
