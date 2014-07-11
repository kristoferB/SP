'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:CreatemodelCtrl
 * @description
 * # CreatemodelCtrl
 * Controller of the spGuiApp
 */
  var CreatemodelCtrl = function ($scope, $modalInstance, spTalker, notificationService) {
    $scope.modelName = '';

    $scope.saveModel = function(givenName) {
      var newModel = new spTalker.model();
      newModel.model = givenName;
      newModel.attributes = {};
      newModel.$save(function(savedModel, putResponseHeaders) {
        notificationService.success('A new model was successfully created');
        spTalker.models.push(savedModel);
        $scope.close();
      }, function(error) {
        notificationService.error('The creation failed with the following error:');
        notificationService.error(error);
      });
      $scope.modelName = '';
    };

    $scope.close = function () {
      $modalInstance.dismiss('cancel');
    };
  };