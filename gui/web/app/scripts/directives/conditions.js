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
      link: function($scope, $element) {
        $scope.guardModel = '';
        $scope.guardInput = '';
        $scope.actionModel = '';

        var thingSuggestions = spTalker.thingsAsStrings,
          lastThingStringBehindCursor = '',
          stateVarSuggestions = [];

        $scope.$watch(function() {
          return spTalker.thingsAsStrings
        }, function(newVal) {
            thingSuggestions = newVal;
        });

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
          var caretPos = getCaretPosition(document.getElementById(guardOrAction + 'Input-'+$scope.item.id));
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
        stateVarMatchExp = /(\w\.)([\w\-]*)$/i;
        function getThingSuggestions() {
          return thingSuggestions;
        }
        createAutoSuggestion(guardInputElement, $scope.guardModel, thingMatchExp, getThingSuggestions, '', '');
        createAutoSuggestion(guardInputElement, $scope.guardModel, stateVarMatchExp, getStateVarSuggestions, 'guard', ' ');
        createAutoSuggestion(actionInputElement, $scope.actionModel, thingMatchExp, getThingSuggestions, '', '');
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
