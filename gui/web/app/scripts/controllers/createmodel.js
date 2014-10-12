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
    newModel.name = givenName;
    newModel.attributes = { attributeTags: {}, children: [] };
    newModel.$save(function(savedModel) {
      notificationService.success('A new model \"' + savedModel.name + '\" was successfully created');
      spTalker.models[savedModel.model] = savedModel;
      spTalker.activeModel = savedModel;
      $modalInstance.close(savedModel);
    }, function() {
      notificationService.error('The model creation failed.');
    });
    $scope.modelName = '';
  };

   $scope.dismiss = function () {
    $modalInstance.dismiss('cancel');
  };
};
