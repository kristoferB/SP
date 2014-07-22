'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:SiteCtrl
 * @description
 * # SitecontrollerCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('SiteCtrl', function ($scope, $routeParams, $location, $rootScope, spTalker, $modal, USER_ROLES, AuthService, Session) {
    $scope.currentUser = null;
    $scope.userRoles = USER_ROLES;
    $scope.isAuthorized = AuthService.isAuthorized;
    $rootScope.vars = {
      isLoginPage : false
    };

    $scope.$watch(function() {
        return $rootScope.vars.isLoginPage;
      }, function(data) {
          $scope.isLoginPage = data;
      }, true);

    $scope.$watch(function() {
        return Session.id;
    }, function(data) {
        $scope.currentUser = [Session.userId, Session.userRole];
    }, true);

    $scope.headerUrl = 'views/header.html';

    $scope.$watch(
      function() { return spTalker.activeModel; },
      function(data) { $scope.activeModel = data; },
      true
    );

    $scope.openModelList = function () {
      var modalInstance = $modal.open({
        templateUrl: 'views/modellist.html',
        controller: ModellistCtrl
      });
    };
    $scope.openModelList();

    $scope.setupSwitch = function () {
      $("[name='model-runtime-switch']").bootstrapSwitch('size', 'small');
      $("[name='model-runtime-switch']").bootstrapSwitch('offText', 'Model');
      $("[name='model-runtime-switch']").bootstrapSwitch('onText', 'Runtime');
      $("[name='model-runtime-switch']").bootstrapSwitch('offColor', 'default');
      $("[name='model-runtime-switch']").bootstrapSwitch('onColor', 'default');
      $("[name='model-runtime-switch']").bootstrapSwitch('state', false);

      $("[name='model-runtime-switch']").on('switchChange.bootstrapSwitch', function (event, state) {
        if (state === true) {
          $location.path('/runtime');
        } else {
          $location.path('/model');
        }
        $location.replace();
        $scope.$apply();
      });
    };

    document.addEventListener('invalid', (function(){
      return function(e){
        //prevent the browser from showing default error bubble/ hint
        e.preventDefault();
        // optionally fire off some custom validation handler
        // myvalidationfunction();
      };
    })(), true);

    $scope.broadcastEvent = function(eventName) {
      $rootScope.$broadcast(eventName);
    };

  });