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
            console.log('Whitespace found');
            ngModel.$setValidity('nowhitespace',false);
          } else {
            console.log('No whitespace found');
            ngModel.$setValidity('nowhitespace',true);
          }

          return viewValue;
        }

        //For DOM -> model validation
        ngModel.$parsers.unshift(noWhitespace);

      }
    };
  });

