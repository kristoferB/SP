'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:disallowDuplicate
 * @description
 * # disallowDuplicate
 */
angular.module('spGuiApp')
  .directive('unique', function (){
    return {
      require: 'ngModel',
      scope: {
        passedObj: '=unique'
      },
      link: function(scope, elem, attr, ngModel) {

        function checkIfExists(viewValue){
          var exists = false;
          scope.passedObj.existingObjects.forEach(function(existingObject) {
            if((viewValue === existingObject[scope.passedObj.propKey]) && (scope.passedObj.editedObj !== existingObject)) {
              exists = true;
            }
          });
          if(exists) {
            ngModel.$setValidity('unique',false);
          } else {
            ngModel.$setValidity('unique',true);
          }
          return viewValue;
        }

        //For DOM -> model validation
        ngModel.$parsers.unshift(checkIfExists);

        //For model -> DOM validation
        ngModel.$formatters.unshift(checkIfExists);
      }
    };
  });
