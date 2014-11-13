'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:SiteCtrl
 * @description
 * # SitecontrollerCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('SiteCtrl', function ($scope, $rootScope, tabSvc, spTalker, $modal, USER_ROLES, AuthService, Session) {
    $scope.currentUser = null;
    $scope.userRoles = USER_ROLES;
    $scope.isAuthorized = AuthService.isAuthorized;
    $scope.currentUser = [Session.userId, Session.userRole];
    $scope.spTalker = spTalker;
    $scope.tabSvc = tabSvc;

    $rootScope.vars = {
      isLoginPage : false
    };
    $scope.isLoginPage = $rootScope.vars.isLoginPage;

    /*$scope.$watch(function() {
        return $rootScope.vars.isLoginPage;
      }, function(data) {
          $scope.isLoginPage = data;
      }, true);*/

    /*$scope.$watch(function() {
        return Session.id;
    }, function(data) {
        $scope.currentUser = [Session.userId, Session.userRole];
    }, true);*/

    $scope.headerUrl = 'views/header.html';

    $scope.openModelList = function () {
      var modalInstance = $modal.open({
        templateUrl: 'views/modellist.html',
        controller: ModellistCtrl
      });
    };

    $scope.openModelHistory = function () {
      var modalInstance = $modal.open({
        templateUrl: 'views/modelhistory.html',
        controller: 'ModelhistoryCtrl'
      });
    };

    if(Object.keys(spTalker.activeModel).length === 0) {
      $scope.openModelList();
    }

    $scope.openFileUpload = function () {
      var modalInstance = $modal.open({
        templateUrl: 'views/loadfile.html',
        controller: 'LoadfileCtrl'
      });
    };

    $scope.openSettings = function () {
      var modalInstance = $modal.open({
        templateUrl: 'views/settings.html',
        controller: 'SettingsCtrl'
      });
    };

    document.addEventListener('invalid', (function(){
      return function(e){
        //prevent the browser from showing default error bubbles/hints on input validation
        e.preventDefault();
      };
    })(), true);

    $scope.broadcastEvent = function(eventName) {
      $rootScope.$broadcast(eventName);
    };

  });
