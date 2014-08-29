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
              var key = e.target.getAttribute('id');
              var value = angular.copy(spTalker.activeSPSpec.attributes.attributeTags[key]);
              console.log(value)
              if (value instanceof Date) {
                value = new Date()
                console.log("new date "+ value)
              } else {
                replaceDates(value)
              }
              if (_.isArray(scope.values.attrObj)) {
                scope.values.attrObj.push(value)
              } else {
                scope.values.attrObj[key] = value
              }
            }
          };
        }

        function replaceDates(obj) {
          for(var k in obj) {
            if(obj.hasOwnProperty(k)) {
              if (obj[k]  instanceof Date){
                obj[k] = new Date();
              } else {
                replaceDates(obj[k]);
              }
            }
          }
        }

        element.contextmenu(attrContextMenu());
      }
    };
  });