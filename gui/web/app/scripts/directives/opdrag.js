'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:opDrag
 * @description
 * # opDrag
 */
angular.module('spGuiApp')
  .directive('opDrag', function () {
    return {
      restrict: 'A',
      scope: {
        id: '=opDrag'
      },
      link: function postLink(scope, element, attrs) {
        function start(ev) {
          ev.dataTransfer.setData('id', scope.id);
        }

        element[0].addEventListener('dragstart', start, false);
      }
    };
  });
