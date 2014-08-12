'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
  .directive('sop', function (sopDrawer, $compile) {
    return {
      restrict: 'E',
      link: function postLink(scope, element, attrs) {
        var paper = Raphael(element[0],100,100);

        scope.$on('redrawSop', function() {
          scope.calcAndDrawSop(false);
        });

        scope.$on('drawSop', function() {
          scope.calcAndDrawSop(true);
        });

        scope.calcAndDrawSop = function(newDraw) {
          sopDrawer.calcAndDrawSop(scope.storage.sopDef, paper, true, newDraw, scope);
        };

        scope.$watch(
          function() { return scope.storage.sopDef.clientSideAdditions.width + scope.storage.sopDef.clientSideAdditions.height },
          function() {
            var squareSideLength;
            if(scope.storage.sopDef.clientSideAdditions.width > scope.storage.sopDef.clientSideAdditions.height) {
              squareSideLength = scope.storage.sopDef.clientSideAdditions.width;
            } else {
              squareSideLength = scope.storage.sopDef.clientSideAdditions.height;
            }
            paper.setSize(squareSideLength, squareSideLength);
          }, true);

      }
    };
  });