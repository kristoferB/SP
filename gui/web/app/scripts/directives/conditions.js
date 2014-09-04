'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:conditions
 * @description
 * # conditions
 */
angular.module('spGuiApp')
  .directive('conditions', function (spTalker, Textcomplete) {
    return {
      templateUrl: 'views/conditions.html',
      restrict: 'E',
      scope: {
        conditions: '=',
        edit: '='
      },
      link: function($scope, $element) {

        $scope.guardModel = '';
        $scope.guardInput = '';
        $scope.actionModel = '';
        var thingSuggestions = spTalker.thingsAsStrings,
          lastThingStringBehindCursor = '',
          stateVarSuggestions = [];

        $scope.removeCondition = function(index) {
          $scope.conditions.splice(index);
        };

        $scope.guardAsText = function(condition){
          return propToText(condition.guard)
        }

        $scope.actionAsText = function(condition){
          console.log(condition)
          return actionFormatter(condition.action)
        }

        // Maybe move these to service. Taken from propparser when testing
        function propToText(prop) {
          var operator;
          if(prop.isa === 'EQ' || prop.isa === 'NEQ') {
            var left = handleProp(prop.left),
              right = handleProp(prop.right);
            if(prop.isa === 'EQ') {
              operator = ' == ';
            } else {
              operator = ' != ';
            }
            return left + operator + right;
          } else if(prop.isa === 'AND' || prop.isa === 'OR') {
            operator = ' ' + prop.isa + ' ';
            var line = '';
            for(var i = 0; i < prop.props.length; i++) {
              if(i > 0) {
                line = line + operator;
              }
              line = line + handleProp(prop.props[i]);
            }
            return line;
          } else if(prop.isa === 'NOT') {
            return '!' + handleProp(prop.p);
          } else {
            return '';
          }
        }
        function handleProp(prop) {
          if(prop.hasOwnProperty('id')) {
            return getThingAndStateVarAsStringFromId(prop.id);
          } else if(prop.hasOwnProperty('isa')) {
            return '(' + propToText(prop) + ')';
          } else {
            return prop;
          }
        }
        function getThingAndStateVarAsStringFromId(idSearchedFor) {
          var matchingThingName = false, matchingStateVarName = '';
          spTalker.things.forEach(function(thing) {
            if(matchingThingName) {
              return;
            }
            thing.stateVariables.forEach(function(stateVariable) {
              if(stateVariable.id === idSearchedFor) {
                matchingStateVarName = stateVariable.name;
                matchingThingName = thing.name;
              }
            })
          });
          return matchingThingName + '.' + matchingStateVarName;
        }
        function actionFormatter(action) {
          var  textLine = '';

          for(var i = 0; i < action.length; i++) {
            if(i > 0) {
              textLine = textLine + '; ';
            }
            textLine = getThingAndStateVarAsStringFromId(action[i].stateVariableID) + ' = ' + action.value;
          }
          return textLine;
        }




        function getCaretPosition(ctrl) {
          var CaretPos = 0;   // IE Support
          if (document.selection) {
            ctrl.focus();
            var Sel = document.selection.createRange();
            Sel.moveStart('character', -ctrl.value.length);
            CaretPos = Sel.text.length;
          }
          // Firefox support
          else if (ctrl.selectionStart || ctrl.selectionStart == '0')
            CaretPos = ctrl.selectionStart;
          return (CaretPos);
        }

        function returnWord(text, caretPos) {
          var preText = text.substring(0, caretPos);
          var textSplitOnDots = preText.split(/\./g);
          textSplitOnDots.pop();
          var textBeforeDot = textSplitOnDots.pop();
          if (typeof textBeforeDot !== 'undefined') {
            return textBeforeDot.split(/\b/g).pop();
          }
          return '';
        }

        function getStateVarSuggestions(guardOrAction) {
          var caretPos = getCaretPosition(document.getElementById(guardOrAction + 'Input-'+$scope.item.id+'-'+$scope.$index));
          var newThingStringBehindCursor = returnWord($scope[guardOrAction + 'Model'], caretPos);
          if(newThingStringBehindCursor !== lastThingStringBehindCursor) {
            lastThingStringBehindCursor = newThingStringBehindCursor;
            var thingBehindCursor = spTalker.things.filter(function (thing) {
              return thing.name.toLowerCase() === lastThingStringBehindCursor.toLowerCase();
            })[0];
            stateVarSuggestions = [];
            if (typeof thingBehindCursor !== 'undefined') {
              thingBehindCursor.stateVariables.forEach(function (stateVar) {
                stateVarSuggestions.push(stateVar.name);
              });
            }
          }
          return stateVarSuggestions;
        }

        var guardInputElement = $element.find('.guard-input'),
        actionInputElement = $element.find('.action-input'),
        thingMatchExp = /(^|AND\s|OR\s|==\s|!=\s)([\w\-]*)$/i,
        actionThingMatchExp = /(^|,\s)([\w\-]*)$/i,
        stateVarMatchExp = /(\w\.)([\w\-]*)$/i;
        function getThingSuggestions() {
          return thingSuggestions;
        }
        createAutoSuggestion(guardInputElement, $scope.guardModel, thingMatchExp, getThingSuggestions, '', '');
        createAutoSuggestion(guardInputElement, $scope.guardModel, stateVarMatchExp, getStateVarSuggestions, 'guard', ' ');
        createAutoSuggestion(actionInputElement, $scope.actionModel, actionThingMatchExp, getThingSuggestions, '', '');
        createAutoSuggestion(actionInputElement, $scope.actionModel, stateVarMatchExp, getStateVarSuggestions, 'action', ' ');

        function createAutoSuggestion(inputElement, inputModel, matchExp, suggestionsFunc, funcParam, postReplace) {
          var textComplete = new Textcomplete(inputElement, [
            {
              match: matchExp,
              search: function(term, callback) {
                callback($.map(suggestionsFunc(funcParam), function(suggestion) {
                  return suggestion.toLowerCase().indexOf(term.toLowerCase()) === 0 ? suggestion : null;
                }));
              },
              index: 2,
              replace: function(suggestion) {
                return '$1' + suggestion + postReplace;
              }
            }
          ]);

          $(textComplete).on({
            'textComplete:select': function (e, value) {
              $scope.$apply(function() {
                inputModel = value
              })
            },
            'textComplete:show': function (e) {
              $(this).data('autocompleting', true);
            },
            'textComplete:hide': function (e) {
              $(this).data('autocompleting', false);
            }
          });
        }
      }

    };
  });
