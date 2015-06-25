'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:EditOPCCtrl
 * @description
 * # EditOPCCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp').controller('EditOPCCtrl', function ($rootScope, $scope, $modalInstance, spTalker, opc) {

  $scope.opc = opc;

  $scope.saveOPC = function() {
    function successHandler() {
      $rootScope.$broadcast('itemsQueried');
      $modalInstance.close();
    }
    spTalker.saveItem(opc, false, successHandler)
  };

   $scope.dismiss = function () {
    $modalInstance.dismiss('cancel');
  };

});
