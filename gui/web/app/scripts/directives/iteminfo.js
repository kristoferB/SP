'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:itemInfo
 * @description
 * # itemInfo
 */
angular.module('spGuiApp')
  .directive('itemInfo', function (spTalker, itemSvc) {
    return {
      templateUrl: 'views/iteminfo.html',
      restrict: 'E',
      scope: {
        item: '=',
        edit: '=',
        addWindow: '=',
        showItemName: '='
      },
      link: function postLink(scope, element, attrs) {
        scope.spTalker = spTalker;
        scope.itemSvc = itemSvc;
      }
    };
  });
