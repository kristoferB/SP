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

        scope.addAction = function(condition) {
          condition.action.push({id: '', value: {isa:"ValueHolder", v: ''}});
        };

        scope.removeAction = function(condition, action) {
          var index = condition.action.indexOf(action);
          condition.action.splice(index, 1);
        };

        scope.emptyGuard = {
          guard: {isa:'EQ', right: {isa:"ValueHolder", v: ''}, left: {isa:"ValueHolder", v: ''}}
        };

        scope.removeCondition = function($index) {
          scope.conditions.splice($index, 1);
        };


      }

    };
  });
