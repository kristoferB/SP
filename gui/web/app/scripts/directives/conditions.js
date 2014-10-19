'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:conditions
 * @description
 * # conditions
 */
angular.module('spGuiApp')
  .directive('conditions', function (itemSvc, spTalker) {
    return {
      templateUrl: 'views/conditions.html',
      restrict: 'E',
      scope: {
        conditions: '=',
        edit: '='
      },
      link: function(scope) {
        scope.spTalker = spTalker;
        scope.guardModel = '';
        scope.guardInput = '';
        scope.actionModel = '';
        scope.itemSvc = itemSvc;

        scope.emptyGuard = {
          guard: {isa:'EQ', right: true, left: true}
        };

        scope.removeCondition = function($index) {
          scope.conditions.splice($index, 1);
        };


      }

    };
  });
