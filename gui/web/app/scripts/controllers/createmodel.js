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
    newModel.attributes = {};
    newModel.$save(function(savedModel) {
      notificationService.success('A new model \"' + savedModel.name + '\" was successfully created');
      spTalker.models[savedModel.model] = savedModel;
      spTalker.activeModel = savedModel;
      createDefaultSPSpec();
    }, function() {
      notificationService.error('The model creation failed.');
    });
    $scope.modelName = '';
  };

  function createDefaultSPSpec() {

    function onItemCreationSuccess(spSpec) {
      spTalker.activeModel.attributes.activeSPSpec = spSpec.id;
      spTalker.activeModel.attributes.children = [];
      spTalker.activeModel.$save({modelID: spTalker.activeModel.model}, function(model) {
        notificationService.success('The new model ' + model.name + ' was successfully saved.');
        $modalInstance.close(model);
      });
    }

    spTalker.createItem('SPSpec', onItemCreationSuccess);
  }

  $scope.dismiss = function () {
    $modalInstance.dismiss('cancel');
  };
};
