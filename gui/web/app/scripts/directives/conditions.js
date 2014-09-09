'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:conditions
 * @description
 * # conditions
 */
angular.module('spGuiApp')
  .directive('conditions', function () {
    return {
      templateUrl: 'views/conditions.html',
      restrict: 'E',
      link: function($scope, $element) {
        $scope.guardModel = '';
        $scope.guardInput = '';
        $scope.actionModel = '';

        $scope.removeCondition = function(item, condition) {
          item.conditions.splice(item.conditions.indexOf(condition));
        };


      }

    };
  });
