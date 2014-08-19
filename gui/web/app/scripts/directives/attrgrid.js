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
        edit: '=',
        attributeTypes: '='
      },
      templateUrl: 'views/attrgrid.html',
      controller: function($scope) {

        $scope.isEmpty = function (obj) {
          return angular.equals({},obj);
        };

        if(typeof $scope.attrObj === 'undefined') {
          $scope.attrObj = {};
        }

        $scope.checkType = function(obj, type, aClass) {
          return typeof obj === type;
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
