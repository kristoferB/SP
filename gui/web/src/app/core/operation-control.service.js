/**
 * Created by Kristofer on 2016-02-20.
 */
(function () {
  'use strict';

  angular
    .module('app.core')
    .factory('operationControlService', operationControlService);

  operationControlService.$inject = ['$q','logger', 'eventService', 'spServicesService', 'restService', 'itemService'];
  /* @ngInject */
  function operationControlService($q, logger, eventService, spServicesService, restService, itemService) {
    var service = {
      connected: false,
      controlServiceID: '',
      state: [],
      resourceTree: [],
      latestMess: {},
      connect: connect,
      execute: execute,
      reset: reset
    };

    activate();

    return service;

    function activate() {
      eventService.addListener('ServiceError', onEvent);
      eventService.addListener('Progress', onEvent);
      eventService.addListener('Response', onEvent);
    }

    function onEvent(ev){
      console.log("control service");
      console.log(ev);


      if (!_.has(ev, 'reqID') || ev.reqID !== service.controlServiceID) return;

      if (_.has(ev, 'attributes.theBus')){
        if (ev.attributes.theBus === 'Connected' && ! service.connected){
          sendTo(service.latestMess, 'subscribe');
        }
        service.connected = ev.attributes.theBus === 'Connected'
      }

      if (_.has(ev, 'attributes.state')){
        service.state = ev.attributes.state;
      }
      if (_.has(ev, 'attributes.resourceTree')){
        service.resourceTree = ev.attributes.resourceTree;
      }
    }

    function updateItems(){
      var its = _.filter(itemService.items, function(o){
        return (angular.isDefined(o.id) && angular.isDefined(service.state[o.id]))
      });
      service.itemState = [];
      _.foreach(its, function(o){
        service.itemState.push({'item': o, 'state': service.state[o.id]})
      })
    }

    function connect(bus, connectionSpecID, resourcesID){
      var mess = {
        'setup': {
          'busIP': bus.ip,
          'publishTopic': bus.publish,
          'subscribeTopic': bus.subscribe
        }
      };
      var conn = {};
      if (angular.isDefined(connectionSpecID)){
        conn.connectionDetails = connectionSpecID
      }
      if (angular.isDefined(resourcesID)){
        conn.resources = resourcesID
      }
      mess.connection = conn;

      sendTo(mess, 'connect').then(function(repl){
        console.log("inside first connection");
        console.log(repl);
        if (messageOK(repl) && _.has(repl, 'reqID')){
          service.controlServiceID = repl.reqID;
        }
      });

      service.latestMess = mess;

    }

    function execute(id, params) {
      var mess = service.latestMess;
      mess.command = {
        'commandType': 'execute',
        'execute': id,
        'parameters': params
      };
      spServicesService.callService('OperationControl',{'data':mess});
    }

    function reset() {
      var mess = service.latestMess;
      mess.command = {
        'commandType': 'reset'
      };
      spServicesService.callService('OperationControl',{'data':mess});
    }

    function sendTo(mess, command){
      mess.command = {'commandType': command};
      var f = spServicesService.callService('OperationControl',{'data':mess});
      console.log("sendTo");
      console.log(f);
      return f
    }
    function messageOK(mess){
      var msg = restService.errorToString(event);
      if (msg !== ""){
        logger.error(msg);
        return false;
      } else return true;
    }


  }

})();
