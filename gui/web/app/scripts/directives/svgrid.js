'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:attrGrid
 * @description
 * # attrGrid
 */
angular.module('spGuiApp')
  .directive('svGrid', function (NAME_PATTERN, itemListSvc) {
    return {
      restrict: 'E',
      scope: {
        svArray : '=',
        edit: '=',
        itemForm: '='
      },
      templateUrl: 'views/svgrid.html',
      controller: function($scope) {
        $scope.namePattern = NAME_PATTERN;
        $scope.itemListSvc = itemListSvc;

        $scope.exists = function(obj) {
          return typeof obj !== 'undefined';
        };

        $scope.checkType = function(obj, type, aClass) {
          return typeof obj === type;
        };

        $scope.deleteSV = function(array, sv) {
          array.splice(array.indexOf(sv),1);
        };
      }

    };
  });
