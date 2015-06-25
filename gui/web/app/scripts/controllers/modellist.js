'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:ModellistCtrl
 * @description
 * # ModellistCtrl
 * Controller of the spGuiApp
 */
  var ModellistCtrl = function ($scope, $modalInstance, spTalker, $modal, notificationService) {
    $scope.spTalker = spTalker;

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
      $modalInstance.close();
      spTalker.loadItems();
      notificationService.success('Model ' + chosenModel.name + ' is now set as active.');
    };
  };