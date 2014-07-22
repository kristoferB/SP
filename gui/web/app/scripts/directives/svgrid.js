'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:attrGrid
 * @description
 * # attrGrid
 */
angular.module('spGuiApp')
  .directive('svGrid', function (RecursionHelper) {
    return {
      restrict: 'E',
      scope: {
        svArray : '=',
        edit: '=',
        attributeTypes: '='
      },
      templateUrl: 'views/svgrid.html',
      controller: function($scope) {

        $scope.checkType = function(obj, type, aClass) {
          return typeof obj === type;
        };

        $scope.deleteSV = function(array, sv) {
          array.splice(array.indexOf(sv),1);
        };
      }
      /*compile: function(element) {
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
