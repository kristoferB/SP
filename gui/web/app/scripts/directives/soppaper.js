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

        scope.calcAndDrawSop = function(newDraw) {
          sopDrawer.calcAndDrawSop(scope.sopSpecCopy, paper, true, newDraw, scope, scope.windowStorage.editable);
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