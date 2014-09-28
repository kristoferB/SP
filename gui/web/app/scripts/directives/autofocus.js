'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:autoFocus
 * @description
 * # autoFocus
 */
angular.module('spGuiApp')
  .directive('autoFocus', function ($timeout) {
    return {
      restrict: 'AC',
      link: function(_scope, _element) {
        $timeout(function() {
          _element[0].select();
        }, 10);
        _scope.$on('itemsOrdered', function() {
          $timeout(function() {
            _element[0].select();
          }, 10);
        });
      }
    };
  });