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
          stateVarSuggestions = [],
          thingBehindCursor = '';

        $scope.$watch(function() {
          return spTalker.thingsAsStrings
        }, function(newVal) {
            thingSuggestions = newVal;
        });

        $scope.getThingBehindCursor = function(guardOrAction) {
          var caretPos = getCaretPosition(document.getElementById(guardOrAction + 'Input-'+$scope.item.id));
          thingBehindCursor = returnWord($scope.guardModel, caretPos);
        };

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
          var index = text.indexOf(caretPos);
          var preText = text.substring(0, caretPos);
          var textSplitOnDots = preText.split(/\./g);
          textSplitOnDots.pop();
          var textBeforeDot = textSplitOnDots.pop();
          if (typeof textBeforeDot !== 'undefined') {
            return textBeforeDot.split(/\b/g).pop();
          }
          return '';
        }

        $scope.$watch(function() {
          return thingBehindCursor;
        }, function(newVal, oldVal) {
          if(newVal !== oldVal) {
            getStateVarSuggestions(newVal);
          }
        });

        var getStateVarSuggestions = function(thingString) {
          var thing = spTalker.things.filter(function(thing) {
            return thing.name.toLowerCase() === thingString.toLowerCase();
          })[0];
          stateVarSuggestions = [];
          if(typeof thing !== 'undefined') {
            thing.stateVariables.forEach(function (stateVar) {
              stateVarSuggestions.push(stateVar.name);
            });
          }
        };


        //-- start of text-complete code --//
        //-- thing-complete for guard --//

        var guardInputElement = $element.find('.guard-input');
        var guardThingComplete = new Textcomplete(guardInputElement, [
          {
            match: /(^|AND\s|OR\s|==\s|!=\s)([\w\-]*)$/i,
            search: function(term, callback) {
              callback($.map(thingSuggestions, function(suggestion) {
                return suggestion.toLowerCase().indexOf(term.toLowerCase()) === 0 ? suggestion : null;
              }));
            },
            index: 2,
            replace: function(suggestion) {
              return '$1' + suggestion;
            }
          }
        ]);

        $(guardThingComplete).on({
          'textComplete:select': function (e, value) {
            $scope.$apply(function() {
              $scope.guardModel = value
            })
          },
          'textComplete:show': function (e) {
            $(this).data('autocompleting', true);
          },
          'textComplete:hide': function (e) {
            $(this).data('autocompleting', false);
          }
        });

        //-- stateVar-complete for guard --//

        var guardStateVarComplete = new Textcomplete(guardInputElement, [
          {
            match: /(\w\.)([\w\-]*)$/i,
            search: function(term, callback) {
              callback($.map(stateVarSuggestions, function(suggestion) {
                return suggestion.toLowerCase().indexOf(term.toLowerCase()) === 0 ? suggestion : null;
              }));
            },
            index: 2,
            replace: function(suggestion) {
              return '$1' + suggestion + ' ';
            }
          }
        ]);

        $(guardStateVarComplete).on({
          'textComplete:select': function (e, value) {
            $scope.$apply(function() {
              $scope.guardModel = value
            })
          },
          'textComplete:show': function (e) {
            $(this).data('autocompleting', true);
          },
          'textComplete:hide': function (e) {
            $(this).data('autocompleting', false);
          }
        });

        //-- thing-complete for action --//

        var actionInputElement = $element.find('.action-input');
        var actionComplete = new Textcomplete(actionInputElement, [
          {
            match: /(^|AND\s|OR\s|==\s|!=\s)([\w\-]*)$/i,
            search: function(term, callback) {
              callback($.map(thingSuggestions, function(suggestion) {
                return suggestion.toLowerCase().indexOf(term.toLowerCase()) === 0 ? suggestion : null;
              }));
            },
            index: 2,
            replace: function(suggestion) {
              return '$1' + suggestion;
            }
          }
        ]);

        $(actionComplete).on({
          'textComplete:select': function (e, value) {
            $scope.$apply(function() {
              $scope.actionModel = value
            })
          },
          'textComplete:show': function (e) {
            $(this).data('autocompleting', true);
          },
          'textComplete:hide': function (e) {
            $(this).data('autocompleting', false);
          }
        });

        //-- stateVar-complete for action --//

        var actionStateVarComplete = new Textcomplete(actionInputElement, [
          {
            match: /(\w\.)([\w\-]*)$/i,
            search: function(term, callback) {
              callback($.map(stateVarSuggestions, function(suggestion) {
                return suggestion.toLowerCase().indexOf(term.toLowerCase()) === 0 ? suggestion : null;
              }));
            },
            index: 2,
            replace: function(suggestion) {
              return '$1' + suggestion + ' ';
            }
          }
        ]);

        $(actionStateVarComplete).on({
          'textComplete:select': function (e, value) {
            $scope.$apply(function() {
              $scope.actionModel = value
            })
          },
          'textComplete:show': function (e) {
            $(this).data('autocompleting', true);
          },
          'textComplete:hide': function (e) {
            $(this).data('autocompleting', false);
          }
        });


        //-- end of text-complete code --//

      }
    };
  });
