'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:loginDialog
 * @description
 * # loginDialog
 */
angular.module('spGuiApp')
  .directive('loginDialog', ['AUTH_EVENTS', '$modal', function (AUTH_EVENTS, $modal) {
    return {
      restrict: 'A',
      template: '<div></div>',
      link: function (scope) {

        scope.openLoginDialog = function () {
          $modal.open({
            templateUrl: 'views/loginform.html',
            controller: LoginCtrl
          });
        };

        scope.$on(AUTH_EVENTS.notAuthenticated, scope.openLoginDialog);
        scope.$on(AUTH_EVENTS.sessionTimeout, scope.openLoginDialog);
      }
    };
  }]);
