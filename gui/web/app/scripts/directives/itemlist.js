'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
.directive('itemlist', ['spTalker', 'notificationService', '$parse', function (spTalker, notificationService, $parse) {
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

      scope.stringToVar = function(item, the_string) {
        console.log(the_string);
        var model = $parse(the_string);  // Get the model
        console.log(model);
        return model;
      };

      scope.createItem = function(type) {
        /*var opArray = [{
          isa : type,
          name : type + ' ' + Math.floor(Math.random()*1000),
          attributes : {}
        }];*/
        var newItem = new spTalker.item({
          isa : type,
          name : type + ' ' + Math.floor(Math.random()*1000),
          attributes : {}
        })
        if(type === 'Operation') {
          newItem.conditions = [];
        };
        if(type === 'Thing') {
          newItem.stateVariables = [];
        };
        if(type === 'SOPSpec') {
          newItem.sop = {isa: 'Sequence', sop: []};
          newItem.version = 1;
        };
        //console.log(angular.copy(newItem));
        newItem.$save(
          {model:spTalker.activeModel.model},
          function(data, putResponseHeaders) { /*console.log(data); console.log(putResponseHeaders);*/ spTalker.items.unshift(data); },
          function(error) { console.log(error); }
        );
        /*spTalker.item.saveArray({model:spTalker.activeModel.model}, opArray,
          function(data) { spTalker.items.push(data[0]); },
          function(error) { console.log(error); }
        );*/
        //newOp.items = [{"name": "Op " , "conditions": [], "attributes": {}, "type": "Operation"}];
      };

      scope.refresh = function() {
        spTalker.loadAll();
        scope.items = spTalker.items;
      };

      scope.saveItem = function(item) {
        console.log(item);
        spTalker.saveItem(item);
      };

      scope.reReadFromServer = function(item) {
        spTalker.reReadFromServer(item);
      };

      scope.isJustViewable = function(key) {
        return key === 'id' || key === 'version' || key === 'isa';
      };

      scope.isPlainlyEditable = function(key) {
        return key !== 'id' && key !== 'version' && key !== 'isa' && key !== 'stateVariables' && key !== 'attributes' && key !== 'sop';
      };

      scope.hasItsOwnEditor = function(key) {
        return key === 'attributes' || key === 'stateVariables' || key === 'sop';
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
