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
      }/*,
      link: function postLink(scope, element, attrs) {

        /*$http.get(tpl)
          .then(function(response){
            element.html($compile(response.data)(scope, function(cloned, scope){
              element.append(cloned);
            }));
          });


      }*/
    };
  });
