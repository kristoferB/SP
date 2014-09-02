'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:ModellistCtrl
 * @description
 * # ModellistCtrl
 * Controller of the spGuiApp
 */
  var ModellistCtrl = function ($scope, $modalInstance, spTalker, $modal, notificationService) {
    $scope.models = spTalker.models;

    $scope.createModel = function () {
      var modalInstance = $modal.open({
        templateUrl: 'views/createmodel.html',
        controller: CreatemodelCtrl
      });

      modalInstance.result.then(function (createdModel) {
        $scope.setActiveModel(createdModel);
      });
    };

    $scope.dismiss = function () {
      $modalInstance.dismiss('cancel');
    };

    $scope.setActiveModel = function (chosenModel) {
      spTalker.activeModel = chosenModel;
      sessionStorage.activeModel = angular.toJson(chosenModel);
      spTalker.loadAll();
      notificationService.success('Model ' + chosenModel.model + ' is now set as active.');
      $modalInstance.close();
    };
  };