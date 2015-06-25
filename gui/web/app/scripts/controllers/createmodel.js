'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:CreatemodelCtrl
 * @description
 * # CreatemodelCtrl
 * Controller of the spGuiApp
 */
var CreatemodelCtrl = function ($scope, $modalInstance, spTalker, notificationService, NAME_PATTERN) {
  $scope.enteredModelName = '';
  $scope.existingModels = spTalker.models;
  $scope.namePattern = NAME_PATTERN;

  $scope.saveModel = function(givenName) {
    function successHandler(createdModel) {
      $scope.modelName = '';
      $modalInstance.close(createdModel);
    }
    spTalker.createModel(givenName, successHandler);
  };

   $scope.dismiss = function () {
    $modalInstance.dismiss('cancel');
  };
};
