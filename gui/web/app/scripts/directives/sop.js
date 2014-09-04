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
          function() { return scope.sopDef[0].clientSideAdditions.width + scope.sopDef[0].clientSideAdditions.height },
          function() {
            var squareSideLength;
            if(scope.sopDef[0].clientSideAdditions.width > scope.sopDef[0].clientSideAdditions.height) {
              squareSideLength = scope.sopDef[0].clientSideAdditions.width;
            } else {
              squareSideLength = scope.sopDef[0].clientSideAdditions.height;
            }
            paper.setSize(squareSideLength, squareSideLength);
          }, true);

      }
    };
  });