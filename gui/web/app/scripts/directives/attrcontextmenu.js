'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:attrContextMenu
 * @description
 * # attrContextMenu
 */
angular.module('spGuiApp')
  .directive('attrContextMenu', function (spTalker) {
    return {
      restrict: 'A',
      scope: {
        attrObj: '=',
        edit: '='
      },
      link: function postLink(scope, element) {

        function attrContextMenu() {
          return {
            target:'#attr-context-menu',
            before: function (e, context) {
              return scope.edit
            },
            onItem: function (context, e) {
              var key = e.target.getAttribute('id');
              scope.attrObj[key] = angular.copy(spTalker.activeSPSpec.attributes.attributeTags[key]);
              replaceDates(scope.attrObj, key);
            }
          };
        }

        function replaceDates(obj, key) {
          if (obj[key] instanceof Date) {
            obj[key] = new Date();
          }
          for(var k in obj[key]) {
            if(obj[key].hasOwnProperty(k)) {
              replaceDates(obj[key], k);
            }
          }
        }

        element.contextmenu(attrContextMenu());
      }
    };
  });