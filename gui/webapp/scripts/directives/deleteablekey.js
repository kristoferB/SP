'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:deleteableKey
 * @description
 * # deleteableKey
 */
angular.module('spGuiApp')
  .directive('deleteableKey', function (itemListSvc) {
    return {
      templateUrl: 'views/deleteablekey.html',
      restrict: 'E',
      scope: {
        attrObj: '=',
        key: '=',
        edit: '=',
        element: '='
      },
      link: function postLink(scope, element, attrs) {
        scope.itemListSvc = itemListSvc;
        scope.menu = {
          isOpen: false
        };
      }
    };
  });
