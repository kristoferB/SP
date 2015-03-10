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
      link: function postLink(scope, element, attrs) {
        var data = scope.$eval(attrs.itemDrag);
        function start(ev) {
          for(var key in data) {
            if (data.hasOwnProperty(key)) {
              ev.dataTransfer.setData(key, data[key]);
            }
          }
        }
        element[0].addEventListener('dragstart', start, false);
      }
    };
  });
