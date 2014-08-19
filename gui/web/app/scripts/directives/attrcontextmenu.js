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
        values: '=attrContextMenu'
      },
      link: function postLink(scope, element) {

        function attrContextMenu() {
          return {
            target:'#attr-context-menu',
            before: function (e, context) {
              return scope.values.edit
            },
            onItem: function (context, e) {
              var index = e.target.getAttribute('id');
              var attrTag = spTalker.activeSPSpec.attributes.attributeTags[index];
              if(attrTag.type === 'string') {
                scope.values.attrObj[attrTag.tag] = '';
              } else {
                scope.values.attrObj[attrTag.tag] = {};
              }
            }
          };
        }

        element.contextmenu(attrContextMenu());
      }
    };
  });