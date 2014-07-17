'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.AuthService
 * @description
 * # AuthService
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('AuthService', function ($http, Session) {
    var authService = {};

    $http.defaults.headers.common['Authorization'] = 'Basic ' + window.btoa('admin' + ':' + 'pass');

    authService.login = function (credentials) {
      return $http
        .post('/api/login', credentials)
        .then(function (res) {
          var data = res.data;
          console.log(data);
          Session.create(data.id, data.id, data.role);
          return data;
        });
    };

    authService.isAuthenticated = function () {
      return !!Session.userId;
    };

    authService.isAuthorized = function (authorizedRoles) {
      if (!angular.isArray(authorizedRoles)) {
        authorizedRoles = [authorizedRoles];
      }
      return (authService.isAuthenticated() &&
        authorizedRoles.indexOf(Session.userRole) !== -1);
    };

    return authService;
  });
