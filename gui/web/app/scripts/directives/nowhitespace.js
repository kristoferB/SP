'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:noWhitespace
 * @description
 * # noWhitespace
 */

angular.module('spGuiApp')
  .value('noWhitespacePattern', new RegExp('\\s'))
  .directive('noWhitespace', function (noWhitespacePattern) {
    return {
      restrict: 'A',
      require: 'ngModel',
      link: function(scope, elem, attr, ngModel) {

        function noWhitespace(viewValue){

          if(noWhitespacePattern.test(viewValue)) {
            ngModel.$setValidity('nowhitespace',false);
          } else {
            ngModel.$setValidity('nowhitespace',true);
          }

          return viewValue;
        }

        //For DOM -> model validation
        ngModel.$parsers.unshift(noWhitespace);

      }
    };
  });

