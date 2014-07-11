'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:LoginCtrl
 * @description
 * # LoginCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('LoginCtrl', function ($scope, $rootScope, AUTH_EVENTS, AuthService) {
    $scope.credentials = {
      username: '',
      password: ''
    };
    $scope.login = function (credentials) {
      AuthService.login(credentials).then(function (user) {
        $rootScope.$broadcast(AUTH_EVENTS.loginSuccess);
        $scope.setCurrentUser(user);
      }, function () {
        $rootScope.$broadcast(AUTH_EVENTS.loginFailed);
      });
    };
  });
