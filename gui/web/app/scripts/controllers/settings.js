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
      var success = true;
      spTalker.activeModel.$save({modelID: spTalker.activeModel.model}, function(data) {}, function(error) {
        console.log(error);
        notificationService.error('An error occurred during save of the active model. Please see your browser console for details.');
        success = false;
      });
      if(success) {
        $scope.close();
        notificationService.success('Settings saved.');
      }
    };

    $scope.reset = function() {
      spTalker.activeModel.$get();
    };

    $scope.close = function() {
      $modalInstance.dismiss('cancel');
    };

  });