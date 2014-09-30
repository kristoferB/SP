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
      restrict: 'A',
      scope: {
        scrollIf: '=',
        contentWrapper: '='
      },
      link: function postLink(scope, element) {

        scope.$watch(
          function() {
            return scope.scrollIf;
          },
          function(newVal) {
            if(newVal === true) {
              // calc how far from top
              var parent = element.context.parentElement;
              var offsetSum = element[0].offsetTop;
              while(parent.classList[0] !== 'content-wrapper') {
                if(parent.nodeName === 'TR') {
                  offsetSum += parent.offsetTop;
                }
                parent = parent.parentElement;
              }
              // smooth scroll
              $(scope.contentWrapper).animate({scrollTop: offsetSum}, 300);

            }
          }
        );

      }
    }
  });