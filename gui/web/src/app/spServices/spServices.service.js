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
      eventQue: {},
      callService: callService,
      listenForServiceEvents: listenForServiceEvents
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





    function getRegisteredSpServices() {
      restService.getRegisteredServices().then(function (data) {
//                logger.info("service" + JSON.stringify(data))
        service.spServices.push.apply(service.spServices, data);
      });
    }

    /**
     * To ask a service.
     * @param spService The service to call
     * @param request The request attributes to send to the service
     * @param answerCallBack function(data) where data is a response or an {error:String}
     * @param progressCallback function(data) where data is a
     * Progress(isa: 'Progress', attributes: {...}, service: String, reqID: ID)
     */
    function callService(spService, request, answerCallBack, progressCallback) {
      // TODO. Should be moved to serviceForm
      var ids = _.map(itemService.selected, function(item){
        return item.id;
      });
      console.log(ids);

      service.eventQue

      var core = {
        'model': modelService.activeModel.id,
        'includeIDAbles': ids,
        'responseToModel': false,
        'onlyResponse': false
      };

      var serviceAttributes = spService.attributes;

      logger.info("Testing service run");
      logger.info(serviceAttributes);
      logger.info(core);

      var idF = restService.getNewID();
      var answerF = idF.then(function(id){
        logger.info('I got id: '+id)
        var sendAttr = {'core': core, reqID: id};
        return restService.postToServiceInstance(sendAttr, spService.name)
      });

      return answerF.then(function(serviceAnswer){
        logger.info('service answer: ' + JSON.stringify(serviceAnswer) + '.');
        return serviceAnswer;
      })

    }
    function listenForServiceEvents(spService, reqID, callBack){

    }


    // Event handler for services
    function onEvent(data){
      console.log("got event");
      console.log(data);

      var s = data.service
      var id = data.reqID


      var current = service.eventQue[id]
      if (_.isUndefined(current)) {
        current = {'service': s, 'reqID': id, events: []};
      }
      current.events.push(data);
      service.eventQue[id] = current;
      console.log(service.eventQue)
    }

  }



})();
