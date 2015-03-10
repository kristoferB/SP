'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:ModelhistoryCtrl
 * @description
 * # ModelhistoryCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('ModelhistoryCtrl', function ($scope, spTalker, $modalInstance) {
    $scope.modelVersions = [];
    $scope.rowSettings = {};
    $scope.spTalker = spTalker;

    for(var i = spTalker.activeModel.version; (spTalker.activeModel.version - i) < 50 && i > 1; i--) {
      var ver = {no: i, diff: spTalker.getModelVersionDiff(i)};
      $scope.modelVersions.push(ver);
    }

    $scope.revert = function(versionNo) {
      spTalker.revertModel(versionNo);
      $scope.dismiss();
    };

    $scope.dismiss = function () {
      $modalInstance.dismiss('cancel');
    };
  });
