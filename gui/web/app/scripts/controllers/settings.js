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
    $scope.attributeTypes = ['string', 'object'];
    $scope.spTalker = spTalker;
    $scope.spSpecs = { array: [] };

    $scope.removeAttributeTag = function(tagToRemove) {
      var index = spTalker.activeSPSpec.attributes.attributeTags.indexOf(tagToRemove);
      spTalker.activeSPSpec.attributes.attributeTags.splice(index, 1);
    };

    $scope.activeSPSpecChange = function() {
      if(typeof spTalker.activeSPSpec.attributes.attributeTags === 'undefined') {
        spTalker.activeSPSpec.attributes.attributeTags = {};
      }
    };

    $scope.save = function () {
      var success = true;
      if(!spTalker.saveItems($scope.spSpecs.array, false)) {
        success = false;
      }
      spTalker.activeModel.attributes.activeSPSpec = spTalker.activeSPSpec.id;
      spTalker.activeModel.$save({model: spTalker.activeModel.model}, function(data) {}, function(error) {
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
      $scope.spSpecs.array.forEach(function(spSpec) {
        spTalker.reReadFromServer(spSpec);
      });
    };

    $scope.close = function() {
      $modalInstance.dismiss('cancel');
    };

  });