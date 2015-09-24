/**
 * Created by patrik on 2015-09-14.
 */
(function () {
  'use strict';

  angular
    .module('app.spServices')
    .factory('spServicesService', spServicesService);

  spServicesService.$inject = ['$q', 'logger', 'restService', 'modelService', 'itemService', 'eventService', '$rootScope'];
  /* @ngInject */
  function spServicesService($q, logger, restService, modelService, itemService, eventService, $rootScope) {
    var service = {
      spServices: [],
      eventQue: {},
      eventCounter: 0,
      callService: callService,
      eventListeners: {},
      getService: getService
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


    function getService(name){
      var f = _.find(service.spServices, function(s){
        return s.name == name
      })
      return f;
    }



    function getRegisteredSpServices() {
      restService.getRegisteredServices().then(function (data) {
//                logger.info("service" + JSON.stringify(data))
        service.spServices.push.apply(service.spServices, data);
      });
    }


    function callService(spService, request, responseCallBack, progressCallback) {
      // TODO. Should be moved to serviceForm
      var ids = _.map(itemService.selected, function(item){
        return item.id;
      });
      console.log(ids);


      var defaultCore = {
        'model': modelService.activeModel.id,
        'includeIDAbles': ids,
        'responseToModel': false,
        'onlyResponse': false
      };

      var message = {};

      if (_.isObject(request.data)){
        message = request.data;
      }

      if (_.isUndefined(message.core)){
        message.core = defaultCore;
      }



      var idF = restService.getNewID();
      var answerF = idF.then(function(id){
        addEventListener(id, responseCallBack, progressCallback);
        message.reqID = id;
        return restService.postToServiceInstance(message, spService.name)
      });

      return answerF.then(function(serviceAnswer){
        logger.info('service answer: ' + JSON.stringify(serviceAnswer) + '.');
        return serviceAnswer;
      })

    }

    function addEventListener(reqID, responseCallBack, progressCallback){
      var current = service.eventListeners[reqID];
      if (_.isUndefined(current)) {
        current = {
          reqID: reqID,
          response: [],
          progress: []
        };
      }
      current.response.push(responseCallBack);
      current.progress.push(catchErrors);

      service.eventListeners[reqID] = current;

      var que = service.eventQue[reqID];
      if (!_.isUndefined(que)) {
        _.forEach(que.events, function(e){
          sendEventToListener(e);
        })
      }

      function catchErrors(event) {
        var msg = restService.errorToString(event)
        if (msg !== ""){
          logger.error(msg);
        }
        progressCallback(event);
      }
    }

    function sendEventToListener(e){
      var current = service.eventListeners[e.reqID];
      if (!_.isUndefined(current)) {
        console.log(current);
        if (!(_.isUndefined(e.isa)) && e.isa == 'Response'){
          _.forEach(current.response, function(cb){
            $rootScope.$apply(cb(e));
          })
        }
        else {
          _.forEach(current.progress, function(cb){
            $rootScope.$apply(cb(e));
          })
        }
      }
    }

    // Event handler for services
    function onEvent(data){
      var s = data.service;
      var id = data.reqID;
      var isRespons = !(_.isUndefined(data.isa)) && data.isa == 'Response';

      var current = service.eventQue[id]
      if (_.isUndefined(current)) {
        current = {
          service: s,
          reqID: id,
          events: [],
          counter: service.eventCounter
        };
        service.eventCounter += 1;
        if (service.eventCounter > 1000) service.eventCounter = 0;
      }

      current.events.push(data);
      if (isRespons){
        current.includesResponse = true;
        service.eventQue = removeOldEventQues(service.eventQue);
        //console.log(service.eventQue);
      }
      service.eventQue[id] = current;

      sendEventToListener(data);
    }


    function removeOldEventQues(que){
      var completed = _.filter(que, function(q){
        return true;
      });

      if (completed.length < 10){return que}

      var sorted = _.sortByAll(completed, ['counter']);
      var noOld = _.takeRight(sorted, 5)
      return _.indexBy(noOld, 'reqID')
    }

  }



})();
