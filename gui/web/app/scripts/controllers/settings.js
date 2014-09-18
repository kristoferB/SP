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
      if(!spTalker.saveItems(spTalker.spSpecs, false)) {
        success = false;
      }
      spTalker.activeModel.attributes.activeSPSpec = spTalker.activeSPSpec.id;
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
      for(var id in spTalker.spSpecs) {
        if(spTalker.spSpecs.hasOwnProperty(id)) {
          spTalker.reReadFromServer(spTalker.spSpecs[id]);
        }
      }
    };

    $scope.close = function() {
      $modalInstance.dismiss('cancel');
    };

  });