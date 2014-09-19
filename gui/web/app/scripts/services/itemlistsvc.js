'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.itemListSvc
 * @description
 * # itemListSvc
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('itemListSvc', ['$rootScope', 'spTalker', 'notificationService', '$timeout', function($rootScope, spTalker, notificationService, $timeout) {

    var factory = {};

    factory.getChildren = function(parentItem, childrenArray) {
      while(childrenArray.length > 0) {
        childrenArray.pop()
      }
      if(typeof parentItem.attributes.children !== 'undefined') {
        parentItem.attributes.children.forEach(function(childId) {
          childrenArray.push(spTalker.getItemById(childId));
        });
      }
    };

    factory.expandChildren = function(row) {
      row.infoIsCollapsed = true;
      row.loadChildren = true;
      $timeout( function() {
        row.expandChildren = !row.expandChildren;
      });
    };

    factory.saveItem = function(item, row) {
      spTalker.saveItem(item, true);
      row.edit = false;
    };

    factory.reReadFromServer = function(item, row) {
      spTalker.reReadFromServer(item);
      row.edit=false;
    };

    factory.deleteItem = function(item) {
      if(confirm('You are about to delete ' + item.name + ' from server. Are you sure?')) {
        spTalker.deleteItem(item);
      }
    };

    factory.createItem = function(type, parentItem) {

      function onItemCreationSuccess(data) {
        var parent;
        if(type === 'SPSpec') {
          $rootScope.$broadcast('itemsQueried');
        } else if(typeof parentItem === 'undefined') {
          parent = spTalker.activeModel;
          if(typeof parent.attributes.children === 'undefined') {
            parent.attributes.children = [];
          }
          parent.attributes.children.push(data.id);
          parent.$save({modelID: spTalker.activeModel.model}, function() {
            $rootScope.$broadcast('itemsQueried');
          });
        } else {
          parent = parentItem;
          if(typeof parent.attributes.children === 'undefined') {
            parent.attributes.children = [];
          }
          parent.attributes.children.push(data.id);
          spTalker.saveItem(parent);
          $rootScope.$broadcast('itemsQueried');
        }
      }

      spTalker.createItem(type, onItemCreationSuccess);
    };

    factory.addCondition = function(condArray) {
      condArray.push({guard: {}, action: [], attributes: {kind: '', group: ''}});
    };

    factory.addStateVar = function(svArray, type) {
      var stateVar = {
        name: 'newVar',
        attributes: {}
      };
      if(type === 'domain') {
        stateVar.attributes[type] = ['home', 'flexlink'];
      } else if(type === 'range') {
        stateVar.attributes[type] = {
          start: 0,
          end: 2,
          step: 1
        };
      } else if(type === 'boolean') {
        stateVar.attributes[type] = true;
      }
      svArray.push(stateVar);
    };

    factory.stopPropagation = function(e) {
      e.stopPropagation();
    };

    factory.shouldBeShown = function(key) {
      return key !== 'checked';
    };

    factory.hasItsOwnEditor = function(key) {
      return key === 'stateVariables' || key === 'sop' || key === 'conditions';
    };

    factory.hasItsOwnViewer = function(key) {
      return key !== 'id' && key !== 'version' && key !== 'isa' && key !== 'name';
    };

    factory.isEditable = function(key) {
      return key !== 'id' && key !== 'version' && key !== 'isa';
    };

    factory.openSopInNewWindow = function(item, addWindow) {
      var windowStorage = {
        sopSpecId: item.id
      };
      addWindow('sopMaker', windowStorage);
    };

    return factory;

  }]);

angular.module('spGuiApp').filter('filterElements', function () {
  return function (input) {
    var filteredInput ={};
    angular.forEach(input, function(value, key){
      if(key !== 'id' && key !=='name' && key !== 'isa' && key !== 'version' && key !== 'attributes' && key !== 'children' && key !== 'attributeTags'){
        filteredInput[key]= value;
      }
    });
    return filteredInput;
  }});
