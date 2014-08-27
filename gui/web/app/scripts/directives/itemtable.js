'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:itemTable
 * @description
 * # itemTable
 */
angular.module('spGuiApp')
  .directive('itemTable', function (itemListSvc) {
    return {
      templateUrl: 'views/itemtable.html',
      restrict: 'E',
      scope: {
        items: '=',
        parentId: '=',
        alterCheckedArray: '=',
        selection: '=',
        attrSelection: '=',
        addWindow: '=',
        getFilterAndOrderItems: '='
      },
      link: function postLink(scope, element, attrs) {
        scope.itemListSvc = itemListSvc;

      }
    };
  });
