'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.AuthInterceptor
 * @description
 * # AuthInterceptor
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('AuthInterceptor', ['$rootScope', '$q', 'AUTH_EVENTS', function ($rootScope, $q, AUTH_EVENTS) {
    return {
      responseError: function (response) {
        /*$rootScope.$broadcast({
          401: AUTH_EVENTS.notAuthenticated,
          403: AUTH_EVENTS.notAuthorized,
          419: AUTH_EVENTS.sessionTimeout,
          440: AUTH_EVENTS.sessionTimeout
        }[response.status], response);*/
        //console.log('Got a ' + response.status + ' status response');
        return $q.reject(response);
      }
    };
  }]);
