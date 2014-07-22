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

      scope.saveItem = function(item) {
        item.$save(
          {model: spTalker.activeModel.model, id: item.id},
          function (data, headers) {
            notificationService.success(item.isa + ' \"' + item.name + '\" was successfully saved');
          },
          function (error) {
            notificationService.error(item.isa + ' ' + item.name + ' could not be saved. ' + error.data);
            console.log(error);
            scope.reReadFromServer(item);
          }
        );
      };

      scope.refresh = function() {
        spTalker.loadAll();
        scope.items = spTalker.items;
      };

      scope.reReadFromServer = function(item) {
        item.$get({model: spTalker.activeModel.model});
      };

      scope.isJustViewable = function(key) {
        return key === 'id' || key === 'version' || key === 'isa';
      };

      scope.isPlainlyEditable = function(key) {
        return key !== 'id' && key !== 'version' && key !== 'isa' && key !== 'stateVariables' && key !== 'attributes';
      };

      scope.hasItsOwnEditor = function(key) {
        return key === 'attributes' || key === 'stateVariables';
      };

      scope.refresh();

    }
  };
}]);
