'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:CreateSopSpecCtrl
 * @description
 * # CreateSopSpecCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('CreateSopSpecCtrl', function ($scope, $modalInstance, NAME_PATTERN) {
    $scope.namePattern = NAME_PATTERN;
    $scope.enteredSOPSpecName = '';

    $scope.save = function () {
      $modalInstance.close($scope.enteredSOPSpecName);
    };

    $scope.dismiss = function() {
      $modalInstance.dismiss('cancel');
    };

  });