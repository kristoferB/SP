/**
 * Created by Martin on 2015-11-19.
 */
(function () {
  'use strict';

  angular
    .module('app.opcRunner')
    .factory('opcRunnerService', opcRunnerService);

  opcRunnerService.$inject = ['$q', 'logger', 'restService', 'modelService', 'itemService', 'eventService', '$rootScope'];
  /* @ngInject */
  function opcRunnerService($q, logger, restService, modelService, itemService, eventService, $rootScope) {
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
