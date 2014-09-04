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
        console.log('parentItem have children');
        parentItem.attributes.children.forEach(function(childId) {
          console.log(childId);
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
      spTalker.saveItem(item);
      row.edit = false;
    };

    factory.reReadFromServer = function(item, row) {
      spTalker.reReadFromServer(item);
      row.edit=false;
    };

    factory.createItem = function(type, parentItem) {

      function onItemCreationSuccess(data) {
        var parent;
        if(typeof parentItem === 'undefined') {
          parent = spTalker.activeModel;
          if(typeof parent.attributes.children === 'undefined') {
            parent.attributes.children = [];
          }
          parent.attributes.children.push(data.id);
          parent.$save({modelID: spTalker.activeModel.model});
        } else {
          parent = parentItem;
          if(typeof parent.attributes.children === 'undefined') {
            parent.attributes.children = [];
          }
          parent.attributes.children.push(data.id);
          spTalker.saveItem(parent);
        }
        $rootScope.$broadcast('itemsQueried');
      }

      spTalker.createItem(type, onItemCreationSuccess);
    };

    factory.addCondition = function(cond) {
      cond.push({guard: {}, action: [], attributes: {}});
      console.log("add condition" + cond)
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
      if(key !== 'id' && key !=='name' && key !== 'isa' && key !== 'version' && key !== 'attributes'){
        filteredInput[key]= value;
      }
    });
    return filteredInput;
  }});