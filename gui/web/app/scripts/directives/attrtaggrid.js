'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:attrTagGrid
 * @description
 * # attrTagGrid
 */
angular.module('spGuiApp')
  .directive('attrTagGrid', function (RecursionHelper, $modal) {
    return {
      restrict: 'E',
      scope: {
        attrTagsObj : '='
      },
      templateUrl: 'views/attrtaggrid.html',
      controller: function($scope) {

        $scope.isEmpty = function (obj) {
          return angular.equals({},obj);
        };

        $scope.checkType = function(obj, type) {
          return typeof obj === type;
        };

        $scope.getType = function(attrValue) {
          if(attrValue instanceof Date) {
            return 'date';
          }
          return typeof attrValue;
        };

        $scope.createAttrTag = function() {
          var modalInstance = $modal.open({
            templateUrl: 'views/createattrtag.html',
            controller: 'CreateAttrTagCtrl',
            resolve: {
              attrTagObj: function () {
                return $scope.attrTagsObj;
              }
            }
          });
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
