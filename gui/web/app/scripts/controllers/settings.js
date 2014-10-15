'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:SettingsCtrl
 * @description
 * # SettingsCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('SettingsCtrl', function ($scope, $modalInstance, spTalker, notificationService) {
    $scope.spTalker = spTalker;
    $scope.attributeTypes = ['string', 'object'];

    $scope.removeAttributeTag = function(tagToRemove) {
      var index = spTalker.activeModel.attributes.attributeTags.indexOf(tagToRemove);
      spTalker.activeModel.attributes.attributeTags.splice(index, 1);
    };

    $scope.save = function () {
      function successHandler() {
        $scope.close();
        notificationService.success('Settings saved.');
      }
      spTalker.saveModel(spTalker.activeModel, successHandler);
    };

    $scope.reset = function() {
      spTalker.loadModel(spTalker.activeModel.model);
    };

    $scope.close = function() {
      $modalInstance.dismiss('cancel');
    };

  });