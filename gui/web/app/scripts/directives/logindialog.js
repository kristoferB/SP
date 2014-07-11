'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:loginDialog
 * @description
 * # loginDialog
 */
angular.module('spGuiApp')
  .directive('loginDialog', function (AUTH_EVENTS) {
    return {
      restrict: 'A',
      template: '<div ng-if="visible" ng-include src="\'views/loginform.html\'">',
      link: function (scope) {
        var showDialog = function () {
          scope.visible = true;
        };

        scope.visible = false;
        scope.$on(AUTH_EVENTS.notAuthenticated, showDialog);
        scope.$on(AUTH_EVENTS.sessionTimeout, showDialog)
      }
    };
  });
