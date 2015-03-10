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
      link: function postLink(scope, element) {
        var paper = Raphael(element[0],100,100);

        scope.$on('redrawSop', function() {
          scope.calcAndDrawSop(false);
        });

        scope.$on('drawSop', function() {
          scope.calcAndDrawSop(true);
        });

        scope.$on('clearPaper', function() {
          paper.clear();
        });

        scope.calcAndDrawSop = function(newDraw) {
          sopDrawer.calcAndDrawSop(scope, paper, true, newDraw);
        };

        scope.$watch(
          function() { return scope.sopSpecCopy.width + scope.sopSpecCopy.height },
          function() {
            paper.setSize(scope.sopSpecCopy.width, scope.sopSpecCopy.height);
          }, true
        );
      }
    };
  });