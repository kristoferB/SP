'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:LoginCtrl
 * @description
 * # LoginCtrl
 * Controller of the spGuiApp
 */
var LoginCtrl = function ($scope, $rootScope, AUTH_EVENTS, AuthService, $modalInstance) {
    $scope.credentials = {
      username: '',
      password: ''
    };

    $scope.login = function (credentials) {
      AuthService.login(credentials).then(function (user) {
        $rootScope.$broadcast(AUTH_EVENTS.loginSuccess);
        $scope.close();
      }, function () {
        $rootScope.$broadcast(AUTH_EVENTS.loginFailed);
      });
    };

    $scope.close = function () {
        $modalInstance.dismiss('cancel');
    };

  };


