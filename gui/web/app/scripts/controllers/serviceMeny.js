'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:ServiceMenyCtrl
 * @description
 * # ServiceMenyCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('ServiceMenyCtrl', function ($scope, $modalInstance, $http) {

    // Get these from the services in the future
    $scope.services = [
      {name: 'Model Transformation', service: 'CreateManufOpsFromProdOpsService'},
      {name: 'A service with no registered actor', service: 'Buu'}
    ];

    $scope.onServiceSelect = function(obj) {
      $http.get('api/services/'+obj.service).success(function(dataFromServer)  {
        console.log("dataFromServer: " + dataFromServer)
      }).error(function(dataFromServer) {
        console.log("Problem to get data");
        console.log("dataFromServer: " + dataFromServer)
      });
      $modalInstance.close("")
    };

  });
