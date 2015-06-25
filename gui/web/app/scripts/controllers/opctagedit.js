'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:OPCTagEditCtrl
 * @description
 * # OPCTagEditCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp').controller('OPCTagEditCtrl', function ($scope, $modalInstance, spTalker, API_URL, runtimeName, opcSpec) {

  $scope.spTalker = spTalker;
  $scope.opcSpec = opcSpec;
  $scope.selectedID = "";

  $scope.save = function(opc) {
    var successHandler = function() {
      $modalInstance.close(opc);
    };
    spTalker.saveItem(opcSpec, true, successHandler);
  };

  $scope.addTag = function() {
    if($scope.selectedID !== "")
      opcSpec.attributes.idsToTags[$scope.selectedID] = "";
  };

  $scope.itemName = function(id) {
    if(spTalker.items.hasOwnProperty(id))
      return spTalker.items[id].name;
    else
      return "Item does not exist";
  };

  $scope.removeTag = function(id) {
    delete opcSpec.attributes.idsToTags[id];
  };

  $scope.notYetAddedItems = function() {
    var all = [];

    for (var name in spTalker.thingsAndOpsByName) {
      if (spTalker.thingsAndOpsByName.hasOwnProperty(name)) {
        all.push(spTalker.thingsAndOpsByName[name].id);
      }
    }

    var common = _.intersection(all, Object.keys(opcSpec.attributes.idsToTags));
    return _.difference(all, common);
  };

  $scope.dismiss = function () {
    $modalInstance.dismiss('cancel');
  };

});
