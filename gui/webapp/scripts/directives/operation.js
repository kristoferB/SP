/**
 * Created by daniel on 2015-06-22.
 */
angular.module('spGuiApp')
.directive('operation', function(spTalker) {
  return {
    templateUrl: 'views/operation.html',
    restrict: 'E',
    scope: {
      opState: '=',
      toggleOp: '='
    },
    link: function postLink(scope) {
      scope.spTalker = spTalker;


    }
  };
});