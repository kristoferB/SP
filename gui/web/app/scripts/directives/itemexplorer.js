'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:itemExplorer
 * @description
 * # itemExplorer
 */
angular.module('spGuiApp')
  .directive('itemExplorer', function (spTalker, itemSvc) {
    return {
      templateUrl: 'views/itemexplorer.html',
      restrict: 'E',
      scope: {
        windowStorage: '='
      },
      link: function postLink(scope) {
        scope.itemSvc = itemSvc;
        scope.edit = false;
        scope.spTalker = spTalker;
        scope.explorerMode = typeof scope.windowStorage.itemID === 'undefined';

        if(scope.explorerMode) {
          scope.$on('edit-in-item-explorer', function() {
            scope.edit = true;
          });
        }

        scope.goBack = function() {
          itemSvc.indexOfViewedItem -= 1;
          sessionStorage.indexOfViewedItem = itemSvc.indexOfViewedItem;
        };

        scope.goForward = function() {
          itemSvc.indexOfViewedItem += 1;
          sessionStorage.indexOfViewedItem = itemSvc.indexOfViewedItem;
        };

        scope.saveItem = function() {
          spTalker.saveItem(spTalker.getItemById(itemSvc.selectedItemsHistory[itemSvc.indexOfViewedItem]), true);
          scope.edit = false;
        };

        scope.enterEdit = function() {
          scope.edit = true;
        };

        scope.cancelEdit = function() {
          spTalker.reReadFromServer(spTalker.getItemById(itemSvc.selectedItemsHistory[itemSvc.indexOfViewedItem]));
          scope.edit = false;
        };
      }
    };
  });
