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

    factory.svKindChange = function(sv) {
      var kind = sv.attributes.svKind;
      delete sv.attributes.domain;
      delete sv.attributes.range;
      delete sv.attributes.boolean;
      if(kind === 'domain') {
        sv.attributes[kind] = ['home', 'flexlink'];
      } else if(kind === 'range') {
        sv.attributes[kind] = {
          start: 0,
          end: 2,
          step: 1
        };
      } else if(kind === 'boolean') {
        sv.attributes[kind] = true;
      }
    };

    factory.getChildren = function(parentItem, childrenArray) {
      while(childrenArray.length > 0) {
        childrenArray.pop()
      }
      if(typeof parentItem.attributes.children !== 'undefined') {
        parentItem.attributes.children.forEach(function(childId) {
          childrenArray.push(spTalker.getItemById(childId));
        });
      }
      if(parentItem.isa === 'Thing') {
        parentItem.stateVariables.forEach(function(sv) {
          childrenArray.push(sv);
        })
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
        spTalker.deleteItem(item);
      }
    };

    factory.createItem = function(type, parentItem, itemListScope) {
      function onItemCreationSuccess(data) {
        var parent;
        if(type === 'SPSpec') {
          $rootScope.$broadcast('itemsQueried');
        } else if(parentItem === false) {
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
        console.log(itemListScope);
        $timeout(function() {
          if(parentItem) {
            itemListScope.$broadcast('show-children-' + parentItem.id);
          }
          $timeout(function() {
            itemListScope.$broadcast('show-info-' + data.id);
          });
        });
      }
      spTalker.createItem(type, onItemCreationSuccess);
    };

    factory.addCondition = function(condArray) {
      condArray.push({guard: {}, action: [], attributes: {kind: 'pre', group: ''}});
    };

    factory.addStateVar = function(thing, itemListScope) {
      var stateVar = {
        name: 'stateVar' + Math.floor(Math.random()*1000),
        attributes: { isa: 'StateVariable' }
      };
      stateVar.attributes.domain = ['home', 'flexlink'];
      stateVar.attributes.svKind = 'domain';
      thing.stateVariables.unshift(stateVar);

      function successHandler(data) {
        $rootScope.$broadcast('itemsQueried');
        $timeout(function() {
          itemListScope.$broadcast('show-children-' + thing.id);
          $timeout(function() {
            itemListScope.$broadcast('show-info-' + data.stateVariables[0].id);
          });
        });
      }

      spTalker.saveItem(thing, false, successHandler);

    };

    factory.deleteStateVar = function(thing, stateVar) {
      function successHandler() {
        $rootScope.$broadcast('itemsQueried');
      }
      if(confirm('You are about to delete StateVar ' + stateVar.name + ' from Thing ' + thing.name + '. Are you sure?')) {
        thing.stateVariables.splice(thing.stateVariables.indexOf(stateVar),1);
        spTalker.saveItem(thing, false, successHandler);
      }
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
      if(key !== 'id' && key !=='name' && key !== 'isa' && key !== 'version' && key !== 'attributes' && key !== 'children' && key !== 'attributeTags' && key !== 'stateVariables' && key !== 'svKind' && key !== 'boolean'){
        filteredInput[key]= value;
      }
    });
    return filteredInput;
  }});