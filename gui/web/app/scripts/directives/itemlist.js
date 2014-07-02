'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
.directive('itemlist', ['spTalker', function (spTalker) {

  return {
    templateUrl: 'views/itemlist.html',
    restrict: 'E',
    scope: {},
    link: function postLink(scope, element, attrs) {

      scope.loadData = function() {
        scope.items = spTalker.items.get({model: 'model1'});
      };

      scope.loadData();

    }
  };
}]);
