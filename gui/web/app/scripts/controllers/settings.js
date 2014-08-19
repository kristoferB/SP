'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:SettingsCtrl
 * @description
 * # SettingsCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('SettingsCtrl', function ($scope, $modalInstance, spTalker) {
    $scope.attributeTypes = ['string', 'object'];
    $scope.spTalker = spTalker;
    $scope.form = {
      enteredAttributeTag: '',
      selectedAttributeType: 'string'
    };

    $scope.addAttributeTag = function() {
      if(typeof spTalker.activeSPSpec.attributes.attributeTags === 'undefined') {
        spTalker.activeSPSpec.attributes.attributeTags = [];
      }
      spTalker.activeSPSpec.attributes.attributeTags.push({tag: $scope.form.enteredAttributeTag, type: $scope.form.selectedAttributeType});
      $scope.form.enteredAttributeTag = '';
    };

    $scope.removeAttributeTag = function(tagToRemove) {
      var index = spTalker.activeSPSpec.attributes.attributeTags.indexOf(tagToRemove);
      spTalker.activeSPSpec.attributes.attributeTags.splice(index, 1);
    };

    $scope.close = function () {
      $modalInstance.dismiss('cancel');
    };

    $scope.save = function () {
      spTalker.saveItem(spTalker.activeSPSpec);
    };
  });