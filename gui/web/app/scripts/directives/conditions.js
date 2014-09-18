'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:conditions
 * @description
 * # conditions
 */
angular.module('spGuiApp')
  .directive('conditions', function (itemSvc) {
    return {
      templateUrl: 'views/conditions.html',
      restrict: 'E',
      scope: {
        conditions: '=',
        edit: '='
      },
      link: function(scope) {

        scope.guardModel = '';
        scope.guardInput = '';
        scope.actionModel = '';
        scope.itemSvc = itemSvc;

        scope.removeCondition = function($index) {
          scope.conditions.splice($index, 1);
        };


      }

    };
  });
