'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
  .directive('sop', function (sopDrawer) {
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
          sopDrawer.calcAndDrawSop(scope.sopDef, paper, true, newDraw, scope);
        };

        scope.$watch(
          function() { return scope.sopDef.clientSideAdditions.width + scope.sopDef.clientSideAdditions.height },
          function() {
            var squareSideLength;
            if(scope.sopDef.clientSideAdditions.width > scope.sopDef.clientSideAdditions.height) {
              squareSideLength = scope.sopDef.clientSideAdditions.width;
            } else {
              squareSideLength = scope.sopDef.clientSideAdditions.height;
            }
            paper.setSize(squareSideLength, squareSideLength);
          }, true);

      }
    };
  });