'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:itemDrag
 * @description
 * # itemDrag
 */
angular.module('spGuiApp')
  .directive('itemDrag', function () {
    return {
      restrict: 'A',
      scope: {
        data: '=itemDrag'
      },
      link: function postLink(scope, element, attrs) {
        function start(ev) {
          for(var key in scope.data) {
            if (scope.data.hasOwnProperty(key)) {
              ev.dataTransfer.setData(key, scope.data[key]);
            }
          }
        }
        element[0].addEventListener('dragstart', start, false);
      }
    };
  });
