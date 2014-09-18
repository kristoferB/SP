'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:propParse
 * @description
 * # propParse
 */
angular.module('spGuiApp')
  .directive('propParse', function (spTalker, itemSvc) {
    return {
      require: 'ngModel',
      link: function postLink(scope, element, attrs, ngModel) {
        var thingAndStateVarMatchExp = /([\w]*\.[\w]*)/gi,
          thingMatchExp = /([\w]*)\./i,
          stateVarMatchExp = /\.([\w]*)/i,
          guardOrAction = scope.guardOrAction;

        function namesToId(viewValue) {
          var internalCopy = angular.copy(viewValue),
            valid = true;

          internalCopy = internalCopy.replace(thingAndStateVarMatchExp, function (thingAndStateVarString) {
            var thingString = thingMatchExp.exec(thingAndStateVarString)[1],
              stateVarString = stateVarMatchExp.exec(thingAndStateVarString)[1];
            var thing = spTalker.thingsByName[thingString.toLowerCase()];
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

          if(guardOrAction === 'guard') {
            parseTextAsProp(internalCopy);
          } else {
            parseTextAsAction(internalCopy);
          }

          return viewValue;
        }

        function parseTextAsAction(viewValue) {
          var words = viewValue.split(' '),
            valid = true, actions = [];
          for(var i = 0; i < words.length; i++) {
            var action = {};
            if(typeof words[i] === 'string' && words[i] !== '=') {
              action.stateVariableID = words[i];
            } else {
              valid = false;
              break;
            }
            i++;
            if(typeof words[i] !== 'string' || words[i] !== '=') {
              valid = false;
              break;
            }
            i++;
            if(typeof words[i] === 'string' && words[i] !== '=' && words[i].length > 0) {
              action.value = words[i].replace(',', '');
            } else {
              valid = false;
              break;
            }
            actions.push(action);
          }
          if(valid) {
            ngModel.$setValidity('actionParse', true);
            scope.condition.action = actions;
          } else {
            ngModel.$setValidity('actionParse', false);
          }
        }

        function parseTextAsProp(viewValue){
          spTalker.parseProposition(viewValue)
            .success(function (data) {
              if(typeof data === 'string' && viewValue !== '') {
                ngModel.$setValidity('propParse',false);
              } else {
                if(typeof viewValue === 'undefined' || viewValue === '') {
                  scope.condition['guard'] = {isa:'EQ', right: true, left: true}; // work-around to enable save of ops as long as backend doesn't accept anything else
                } else {
                  scope.condition['guard'] = data;
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
        ngModel.$parsers.unshift(namesToId);

        function conditionFormatter() {
          if(guardOrAction === 'guard') {
            return itemSvc.guardAsText(scope.condition.guard);
          } else {
            return itemSvc.actionAsText(scope.condition.action);
          }
        }

        //For model -> DOM validation
        ngModel.$formatters.unshift(conditionFormatter);


      }
    };
  });
