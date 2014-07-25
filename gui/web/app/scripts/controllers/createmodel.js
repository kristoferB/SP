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
    var newModel = new spTalker.model();
    newModel.model = givenName;
    newModel.attributes = {};
    newModel.$save(function(savedModel, putResponseHeaders) {
      notificationService.success('A new model \"' + savedModel.model + '\" was successfully created');
      spTalker.models.push(savedModel);
      $scope.close();
    }, function(error) {
      notificationService.error('The model creation failed.');
    });
    $scope.modelName = '';
  };

  $scope.close = function () {
    $modalInstance.dismiss('cancel');
  };
};
