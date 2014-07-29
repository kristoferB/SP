'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:conditions
 * @description
 * # conditions
 */
angular.module('spGuiApp')
  .directive('conditions', function (spTalker, $filter, Textcomplete) {
    return {
      templateUrl: 'views/conditions.html',
      restrict: 'E',
      link: function($scope, $element) {
        $scope.conditionsInput = null;
        $scope.conditionsModel = '';
        $scope.parseResponse = '';

        $scope.things = ['R1', 'R2', 'FlexLink'];

          /*$filter('filter')(spTalker.items, function(item) {
          return item.isa === 'Thing'
        });*/

        //-- start of text-complete code --//

        var mentions = ['R1', 'R2', 'FlexLink'];
        var ta = $element.find('input');
        var textcomplete = new Textcomplete(ta, [
          {
            match: /(^|AND\s|OR\s|==\s|!=\s)([\w\-]*)$/i,
            search: function(term, callback) {
              callback($.map(mentions, function(mention) {
                return mention.toLowerCase().indexOf(term.toLowerCase()) === 0 ? mention : null;
              }));
            },
            index: 2,
            replace: function(mention) {
              return '$1' + mention + ' ';
            }
          }
        ]);

        $(textcomplete).on({
          'textComplete:select': function (e, value) {
            $scope.$apply(function() {
              $scope.conditionsModel = value
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
