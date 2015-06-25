'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:propParse
 * @description
 * # propParse
 */
angular.module('spGuiApp')
  .directive('propParse', function (spTalker, itemSvc, itemListSvc) {
    return {
      require: 'ngModel',
      link: function postLink(scope, element, attrs, ngModel) {
        var thingAndStateVarMatchExp = /([\w]*\.[\w]*)/gi,
          thingMatchExp = /([\w]*)/i,
          stateVarMatchExp = /\.([\w]*)/i,
          wordMatchExp = /\b[a-zA-Z0-9.]+\b/gi,
          guardOrAction = scope.guardOrAction;

        function namesToId(viewValue) {
          var internalCopy = angular.copy(viewValue),
            valid = true;

          internalCopy = internalCopy.replace(wordMatchExp, function(word) {
            var id;

            if(thingAndStateVarMatchExp.test(word)) {
              id = resolveDotSeparatedWords(word);
            } else {
              id = resolveWord(word);
            }

            if(id) {
              return id;
            } else {
              return word;
            }

            function resolveWord(word) {
              var thing = spTalker.thingsAndOpsByName[word.toLowerCase()];
              if (typeof thing !== 'undefined') {
                return thing.id;
              }
              return false;
            }

            function resolveDotSeparatedWords(dotSeparatedWords) {
              var wordBeforeDot = thingMatchExp.exec(dotSeparatedWords)[1],
                wordAfterDot = stateVarMatchExp.exec(dotSeparatedWords)[1];
              var thing = spTalker.thingsAndOpsByName[wordBeforeDot.toLowerCase()];
              if (typeof thing !== 'undefined') {
                if(typeof wordAfterDot !== 'undefined') {
                  var children = [];
                  itemListSvc.getChildren(thing, children);
                  var stateVar = children.filter(function (child) {
                    return child.isa === 'Thing' && child.name.toLowerCase() === wordAfterDot.toLowerCase();
                  })[0];
                  if (typeof stateVar !== 'undefined') {
                    return stateVar.id;
                  }
                }
              }
              valid = false;
              return false;
            }

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
              action.id = words[i];
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
          } else if(viewValue === '') {
            ngModel.$setValidity('actionParse', true);
            scope.condition.action = [];
          } else {
            ngModel.$setValidity('actionParse', false);
          }
        }

        function parseTextAsProp(viewValue){
          spTalker.parseProposition(viewValue)
            .success(function (data) {
              if((_.has(data, 'error') ||  typeof data === 'string') && viewValue !== '') {
                ngModel.$setValidity('propParse',false);
              } else {
                if(typeof viewValue === 'undefined' || viewValue === '') {
                  scope.condition['guard'] = {isa:'EQ', right: true, left: true}; // work-around to enable save of ops as long as backend doesn't accept anything else
                } else {
                  scope.condition.guard = data;
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
