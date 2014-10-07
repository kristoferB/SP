'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:scrollIf
 * @description
 * # scrollIf
 */
angular.module('spGuiApp')
  .directive('scrollIf', function ($timeout) {
    return {
      restrict: 'E',
      scope: {
        scrollIf: '='
      },
      link: function postLink(scope, element) {
        scope.$watch(scope.scrollIf, function() {
          console.log('change detected in scrollIf statement');
          if(scope.scrollIf) {
            console.log('scrolling');
            window.scrollTo(0, element[0].offsetTop - 100);
          }
        });

      }
    };
  });