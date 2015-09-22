/**
 * Created by patrik on 2015-09-14.
 */
(function () {
    'use strict';

    angular
        .module('app.spServices')
        .factory('spServicesService', spServicesService);

    spServicesService.$inject = ['$q', 'logger', 'restService', 'modelService', 'itemService', 'eventService'];
    /* @ngInject */
    function spServicesService($q, logger, restService, modelService, itemService, eventService) {
        var service = {
            spServices: [],
            startSpService: startSpService
        };

        activate();

        return service;

        function activate() {

          eventService.addListener('ServiceError', onEvent);
          eventService.addListener('Progress', onEvent);
          eventService.addListener('Response', onEvent);

          var promises = [getRegisteredSpServices()];
          return $q.all(promises).then(function() {
              logger.info('spServices service: Loaded ' + service.spServices + ' spServices through REST.');
          });
          
        }

        // test
        function onEvent(data){
          console.log("got event");
          console.log(data)
        }

        function getRegisteredSpServices() {
            restService.getRegisteredServices().then(function (data) {
//                logger.info("service" + JSON.stringify(data))
                service.spServices.push.apply(service.spServices, data);
            });
        }

        function startSpService(spService) {
//            logger.info("sp services - service: Started service " + spService.name)

          // Core should be prefilled but possible to change in the form
          var ids = _.map(itemService.selected, function(item){
            return item.id;
          });
          console.log(ids);

            var core = {
                'model': modelService.activeModel.id,
                'includeIDAbles': ids,
                'responseToModel': false,
                'onlyResponse': false
            };

            var serviceAttributes = spService.attributes;

            logger.info("Tesing service run");
            logger.info(serviceAttributes);
            logger.info(core);

            var sendAttr = {'core': core};

            restService.postToServiceInstance(sendAttr, spService.name).then(success);

            function success(data){
                logger.info('service answer: ' + JSON.stringify(data) + '.');
            }

        }

    }
})();
