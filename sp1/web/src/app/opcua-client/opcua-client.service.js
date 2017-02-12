/**
 * Created by Martin on 2015-11-19.
 */
(function () {
  'use strict';

  angular
    .module('app.opcuaClient')
    .factory('opcuaClientService', opcuaClientService);

  opcuaClientService.$inject = ['$q', 'logger', 'restService', 'modelService', 'itemService', 'eventService', '$rootScope'];
  /* @ngInject */
  function opcuaClientService($q, logger, restService, modelService, itemService, eventService, $rootScope) {
    var service = {
      x: 1
    };

    activate();

    return service;

    function activate() {
        service.x = 2;
    }
  }

})();
