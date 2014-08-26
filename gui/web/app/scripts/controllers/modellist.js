'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:ModellistCtrl
 * @description
 * # ModellistCtrl
 * Controller of the spGuiApp
 */
  var ModellistCtrl = function ($scope, $modalInstance, spTalker, $modal) {
    $scope.models = spTalker.models;

    $scope.createModel = function () {
      var modalInstance = $modal.open({
        templateUrl: 'views/createmodel.html',
        controller: CreatemodelCtrl
      });
    };

    $scope.close = function () {
      $modalInstance.dismiss('cancel');
    };

    $scope.setActiveModel = function (chosenModel) {
      spTalker.activeModel = chosenModel;
      sessionStorage.activeModel = angular.toJson(chosenModel);
      spTalker.loadAll();
      $scope.close();
    };
  };