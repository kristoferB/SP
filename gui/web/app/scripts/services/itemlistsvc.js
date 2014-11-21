'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.itemListSvc
 * @description
 * # itemListSvc
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('itemListSvc', ['$rootScope', 'itemSvc', 'spTalker', 'notificationService', '$timeout', 'SV_KINDS', function($rootScope, itemSvc, spTalker, notificationService, $timeout) {

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

    factory.createItem = function(type, rootItem, itemListScope) {
      function onItemCreationSuccess(data) {
        var parent;
        if(spTalker.getItemById(itemListScope.windowStorage.selectedItemID)) {
          parent = spTalker.getItemById(itemListScope.windowStorage.selectedItemID);
        } else {
          parent = rootItem;
        }
        if (type === 'SPSpec' || (!parent && !rootItem)) {
          $rootScope.$broadcast('itemsQueried');
        } else {
          if(typeof parent.attributes.children === 'undefined') {
            parent.attributes.children = [];
          }
          parent.attributes.children.push(data.id);
          spTalker.saveItem(parent, false, function() {
            $rootScope.$broadcast('itemsQueried');
          });
        }
        if(itemListScope.windowStorage.itemTree) {
          itemSvc.selectItemId(data.id, itemListScope);
          $rootScope.$broadcast('edit-in-item-explorer');
        } else {
          $timeout(function () {
            if(parent) {
              itemListScope.$broadcast('show-children-' + parent.id);
            }
            $timeout(function () {
              itemListScope.$broadcast('show-info-' + data.id);
            });
          }, 200);
        }

      }
      spTalker.createItem(type, onItemCreationSuccess, null, false, rootItem);
    };

    factory.addCondition = function(condArray) {
      condArray.push({guard: { isa: 'EQ', left: {id: ''}, right: '' }, action: [], attributes: {kind: 'pre', group: 'default'}});
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

    return factory;

  }]);
