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
    $scope.form = {
      enteredAttributeTag: '',
      selectedAttributeType: 'string'
    };

    $scope.removeAttributeTag = function(tagToRemove) {
      var index = spTalker.activeSPSpec.attributes.attributeTags.indexOf(tagToRemove);
      spTalker.activeSPSpec.attributes.attributeTags.splice(index, 1);
    };

    $scope.save = function () {
      var success = true;
      if(!spTalker.saveItems($scope.spSpecs.array, false)) {
        success = false;
      }
      spTalker.activeModel.attributes.activeSPSpec = spTalker.activeSPSpec.id;
      if(!spTalker.updateModelAttributes(false)) {
        success = false;
      }
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