'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:attrGrid
 * @description
 * # attrGrid
 */
angular.module('spGuiApp')
  .directive('attrGrid', function (RecursionHelper) {
    return {
      restrict: 'E',
      scope: {
        attrObj : '=',
        edit: '='
      },
      templateUrl: 'views/attrgrid.html',
      controller: function($scope) {

        $scope.isEmpty = function (obj) {
          return angular.equals({},obj);
        };

        if(typeof $scope.attrObj === 'undefined') {
          $scope.attrObj = {};
        }

        $scope.getType = function(obj) {
          if(obj instanceof Date) {
            return 'date';
          }
          return typeof obj;
        };

        $scope.deleteObjProp = function(obj, prop) {
          delete obj[prop];
        };
      },
      compile: function(element) {
        // Use the compile function from the RecursionHelper,
        // And return the linking function(s) which it returns

        return RecursionHelper.compile(element);
      }

    };
  });
