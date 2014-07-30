'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
.directive('itemlist', ['spTalker', 'notificationService', '$parse', function (spTalker, notificationService, $parse, NAME_PATTERN) {
  return {
    templateUrl: 'views/itemlist.html',
    restrict: 'E',
    link: function postLink(scope, element, attrs) {
      scope.items = [];
      scope.showableColumns = ['name', 'isa', 'version', 'conditions', 'stateVariables', 'attributes']
      scope.selection = ['name', 'isa', 'version'];
      scope.attributeTypes = ['user', 'date', 'comment'];
      scope.attrSelection = angular.copy(scope.attributeTypes);

      scope.toggleSelection = function toggleSelection(column, selections) {
        var idx = selections.indexOf(column);
        // is currently selected
        if (idx > -1) {
          selections.splice(idx, 1);
        }
        // is newly selected
        else {
          selections.push(column);
        }
      };

      scope.createItem = function(type) {
        var newItem = new spTalker.item({
          isa : type,
          name : type + ' ' + Math.floor(Math.random()*1000),
          attributes : {}
        });
        if(type === 'Operation') {
          newItem.conditions = [];
        } else if(type === 'Thing') {
          newItem.stateVariables = [];
        } else if(type === 'SOPSpec') {
          newItem.sop = {isa: 'Sequence', sop: []};
          newItem.version = 1;
        }
        newItem.$save(
          {model:spTalker.activeModel.model},
          function(data) { spTalker.items.unshift(data); },
          function(error) { console.log(error); }
        );
      };

      scope.refresh = function() {
        spTalker.loadAll();
        scope.items = spTalker.items;
      };

      scope.saveItem = function(item, row) {
        spTalker.saveItem(item);
        row.edit = false;
      };

      scope.reReadFromServer = function(item) {
        spTalker.reReadFromServer(item);
      };

      scope.hasItsOwnEditor = function(key) {
        return key === 'attributes' || key === 'stateVariables' || key === 'sop' || key === 'conditions';
      };

      scope.hasItsOwnViewer = function(key) {
        return key !== 'id' && key !== 'version' && key !== 'isa' && key !== 'name';
      };

      scope.isEditable = function(key) {
        return key !== 'id' && key !== 'version' && key !== 'isa';
      };

      scope.openSopInNewWindow = function(item) {
        var windowStorage = {
          sopDef : angular.copy(item.sop),
          parentItem : item
        };
        scope.addWindow('sop', windowStorage, item);
      };

      scope.refresh();

    }
  };
}]);
