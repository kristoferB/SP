'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:ServiceMenyCtrl
 * @description
 * # ServiceMenyCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('ServiceMenyCtrl', function ($scope, $modalInstance, $http, spTalker, notificationService) {

    $scope.services = [];
    getServices();

    $scope.onServiceSelect = function(obj) {
      spTalker.postService(obj);
      $modalInstance.close("")
    };

    function getServices() {
      $http.get('api/services').success(function(dataFromServer)  {
        $scope.services = dataFromServer;
      }).error(function(dataFromServer) {
        notificationService.error('Services could not be loaded. \n' + dataFromServer);
      });
    };

  });
