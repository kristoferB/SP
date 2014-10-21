'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.itemListSvc
 * @description
 * # itemListSvc
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('itemListSvc', ['$rootScope', 'spTalker', 'notificationService', '$timeout', 'SV_KINDS', function($rootScope, spTalker, notificationService, $timeout) {

    var factory = {};

    factory.removeAttribute = function(attrObj, key, menu) {
      menu.isOpen = false;
      if(attrObj instanceof Array) {
        var index = attrObj.indexOf(key);
        attrObj.splice(index, 1);
      } else {
        delete attrObj[key];
      }
    };

    factory.addAttribute = function(attrObj, key, value) {
      attrObj[key] = angular.copy(value);
      replaceDates(attrObj, key);
    };

    function replaceDates(obj, key) {
      if(obj[key] instanceof Date) {
        obj[key] = new Date();
      }
      for(var k in obj[key]) {
        if(obj[key].hasOwnProperty(k)) {
          replaceDates(obj[key], k);
        }
      }
    }

    factory.getChildren = function(parentItem, childrenArray) {
      while(childrenArray.length > 0) {
        childrenArray.pop()
      }
      if(typeof parentItem.attributes !== 'undefined') {
        if (typeof parentItem.attributes.children !== 'undefined') {
          parentItem.attributes.children.forEach(function (childId) {
            var child = spTalker.getItemById(childId);
            if(typeof child === 'undefined') {
              var parentName;
              if(typeof parentItem.name === 'undefined') {
                parentName = 'Model ' + parentItem.model;
              } else {
                parentName = 'Item ' + parentItem.name;
              }
              console.log(parentName + ' contains a reference to a child with id ' + childId + ' which doesn\'t exist');
            } else {
              childrenArray.push(child);
            }
          });
        }
      }
    };

    factory.expandChildren = function(row, collapseIfExpanded) {
      row.infoIsCollapsed = true;
      row.loadChildren = true;
      $timeout( function() {
        if(collapseIfExpanded) {
          row.expandChildren = !row.expandChildren;
        } else {
          row.expandChildren = true;
        }

      });
    };

    factory.saveItem = function(item, row) {
      spTalker.saveItem(item, true);
      row.edit = false;
    };

    factory.reReadFromServer = function(item, row) {
      spTalker.reReadFromServer(item);
      row.edit = false;
    };

    factory.deleteItem = function(item) {
      if(confirm('You are about to delete ' + item.name + ' completely. Are you sure?')) {
        spTalker.deleteItem(item, true);
      }
    };

    factory.createItem = function(type, parentItem, itemListScope) {
      function onItemCreationSuccess(data) {
        if(type === 'SPSpec' || !parentItem) {
          $rootScope.$broadcast('itemsQueried');
        } else {
          if(typeof parentItem.attributes.children === 'undefined') {
            parentItem.attributes.children = [];
          }
          parentItem.attributes.children.push(data.id);
          spTalker.saveItem(parentItem, false, function() {
            $rootScope.$broadcast('itemsQueried');
          });
        }
        $timeout(function () {
          if(parentItem) {
            itemListScope.$broadcast('show-children-' + parentItem.id);
          }
          $timeout(function () {
            itemListScope.$broadcast('show-info-' + data.id);
          });
        }, 100);
      }
      spTalker.createItem(type, onItemCreationSuccess, null, false, parentItem);
    };

    factory.addCondition = function(condArray) {
      var group = '';
      if(spTalker.activeModel.attributes.conditionGroups.length > 0) {
        group = spTalker.activeModel.attributes.conditionGroups[0];
      }
      condArray.push({guard: {}, action: [], attributes: {kind: 'pre', group: group}});
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