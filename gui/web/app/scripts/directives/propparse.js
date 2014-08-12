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
        var thingAndStateVarMatchExp = /([\w]*\.[\w]*)/gi,
          thingMatchExp = /([\w]*)\./i,
          stateVarMatchExp = /\.([\w]*)/i,
          disallowedWordExp = /(AND)/gi;

        function nameToIds(viewValue) {
          var internalCopy = angular.copy(viewValue),
            valid = true,
            words = internalCopy.split();

          internalCopy = internalCopy.replace(thingAndStateVarMatchExp, function (thingAndStateVarString) {
            var thingString = thingMatchExp.exec(thingAndStateVarString)[1],
              stateVarString = stateVarMatchExp.exec(thingAndStateVarString)[1];
            console.log(thingString);
            console.log(stateVarString);
            var thing = spTalker.things.filter(function (aThing) {
              return aThing.name.toLowerCase() === thingString.toLowerCase();
            })[0];
            if (typeof thing !== 'undefined') {
              if(typeof stateVarString !== 'undefined') {
                var stateVar = thing.stateVariables.filter(function (aStateVar) {
                  return aStateVar.name.toLowerCase() === stateVarString.toLowerCase();
                })[0];
                if (typeof stateVar !== 'undefined') {
                  return stateVar.id;
                } else {
                  valid = false;
                }
              } else {
                valid = false;
              }
            } else {
              valid = false;
            }
            return '';
          });

          if(valid) {
            ngModel.$setValidity('nameToIds',true);
          } else {
            ngModel.$setValidity('nameToIds',false);
          }

          console.log(internalCopy);

          parseProposition(internalCopy);

          return viewValue;

        }


        if(scope.passedObj.item[scope.passedObj.key].length === 0) {
          scope.passedObj.item[scope.passedObj.key].push({guard: {}, action: [], attributes: {}});
        }

        function parseProposition(viewValue){
          spTalker.parseProposition(viewValue)
            .success(function (data, status, headers, config) {
              if(typeof data === 'string' && viewValue !== '') {
                ngModel.$setValidity('propParse',false);
              } else {
                if(viewValue === '') {
                  scope.passedObj.item[scope.passedObj.key][0][scope.passedObj.guardOrAction] = {}; // work-around to enable save of ops as long as backend doesn't accept anything else
                } else {
                  scope.passedObj.item[scope.passedObj.key][0][scope.passedObj.guardOrAction] = data;
                  console.log(data);
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
        ngModel.$parsers.unshift(nameToIds);

        /*For model -> DOM validation
        ngModel.$formatters.unshift(nameToIds);*/
      }
    };
  });
