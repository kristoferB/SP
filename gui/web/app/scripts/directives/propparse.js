'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:propParse
 * @description
 * # propParse
 */
angular.module('spGuiApp')
  .directive('propParse', function (spTalker) {
    return {
      require: 'ngModel',
      scope: {
        passedObj: '=propParse'
      },
      link: function postLink(scope, element, attrs, ngModel) {

        function parseProposition(viewValue){
          spTalker.parseProposition(viewValue)
            .success(function (data, status, headers, config) {
              if(typeof data === 'string' && viewValue !== '') {
                ngModel.$setValidity('propParse',false);
              } else {
                if(viewValue === '') {
                  scope.passedObj.item[scope.passedObj.key] = []; // work-around to enable save of ops as long as backend doesn't accept anything else
                } else {
                  scope.passedObj.item[scope.passedObj.key] = data;
                }
                ngModel.$setValidity('propParse',true);
              }
            })
            .error(function (data, status, headers, config) {
              ngModel.$setValidity('propParse',false);
            });
          return viewValue;
        }

        //For DOM -> model validation
        ngModel.$parsers.unshift(parseProposition);

        //For model -> DOM validation
        ngModel.$formatters.unshift(parseProposition);
      }
    };
  });
