'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:attrContextMenu
 * @description
 * # attrContextMenu
 */
angular.module('spGuiApp')
  .directive('createContextMenu', function (itemListSvc) {
    return {
      restrict: 'A',
      link: function postLink(scope, element) {

        function createContextMenu() {
          return {
            target:'#create-context-menu',
            onItem: function (context, e) {
              var key = e.target.getAttribute('id');
              itemListSvc.createItem(key, scope.item);
            }
          };
        }


        element.contextmenu(createContextMenu());
      }
    };
  });