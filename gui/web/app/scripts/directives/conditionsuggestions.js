'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:conditionSuggestions
 * @description
 * # conditionSuggestions
 */
angular.module('spGuiApp')
  .directive('conditionSuggestions', function (spTalker, Textcomplete, itemListSvc) {
    return {
      restrict: 'A',
      scope: {
        ngModel: '='
      },
      link: function postLink(scope, element, attrs) {

        var lastThingStringBehindCursor = '',
          stateVarSuggestions = [];

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

        function getStateVarSuggestions() {
          var caretPos = getCaretPosition(element[0]);
          var newThingStringBehindCursor = returnWord(scope.ngModel, caretPos);
          if(newThingStringBehindCursor !== lastThingStringBehindCursor) {
            lastThingStringBehindCursor = newThingStringBehindCursor;
            var thingBehindCursor = spTalker.thingsAndOpsByName[lastThingStringBehindCursor];
            stateVarSuggestions = [];
            if (typeof thingBehindCursor !== 'undefined') {
              var children = [];
              itemListSvc.getChildren(thingBehindCursor, children);
              children.forEach(function (child) {
                if(child.isa === 'Thing') {
                  stateVarSuggestions.push(child.name);
                }
              });
            }
          }
          return stateVarSuggestions;
        }

        var thingMatchExp = /(^|AND\s|OR\s|==\s|!=\s)([\w\-]*)$/i,
          actionThingMatchExp = /(^|,\s)([\w\-]*)$/i,
          stateVarMatchExp = /(\w\.)([\w\-]*)$/i;
        function getThingSuggestions() {
          return Object.keys(spTalker.thingsAndOpsByName);
        }

        if(scope.guard) {
          createAutoSuggestion(element, scope.ngModel, thingMatchExp, getThingSuggestions, '');
          createAutoSuggestion(element, scope.ngModel, stateVarMatchExp, getStateVarSuggestions, ' ');
        } else {
          createAutoSuggestion(element, scope.ngModel, actionThingMatchExp, getThingSuggestions, '');
          createAutoSuggestion(element, scope.ngModel, stateVarMatchExp, getStateVarSuggestions, ' ');
        }

        function createAutoSuggestion(inputElement, inputModel, matchExp, suggestionsFunc, postReplace) {
          var textComplete = new Textcomplete(inputElement, [
            {
              match: matchExp,
              search: function(term, callback) {
                callback($.map(suggestionsFunc(), function(suggestion) {
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
              scope.$apply(function() {
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
