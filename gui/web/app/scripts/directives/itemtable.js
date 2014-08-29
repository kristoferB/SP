'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:itemTable
 * @description
 * # itemTable
 */
angular.module('spGuiApp')
  .directive('itemTable', function (itemListSvc, spTalker, RecursionHelper) {
    return {
      templateUrl: 'views/itemtable.html',
      restrict: 'E',
      scope: {
        parentItem: '=',
        recievedItems: '=',
        alterCheckedArray: '=',
        selection: '=',
        attrSelection: '=',
        addWindow: '=',
        getFilterAndOrderItems: '='
      },
      controller: function($scope) {
        $scope.itemListSvc = itemListSvc;
        $scope.items = [];

        $scope.$on('itemsQueried', function($event) {
          console.log('itemTable recieved itemsQueried event');
          itemListSvc.getChildren($scope.parentItem, $scope.items);
          $scope.getFilterAndOrderItems();
        });

        if(typeof $scope.recievedItems === 'undefined') {
          console.log($scope.recievedItems);
          itemListSvc.getChildren($scope.parentItem, $scope.items);
          console.log($scope.items);
        } else {
          console.log($scope.recievedItems);
          $scope.items = $scope.recievedItems;
        }

      },
      compile: function(element) {
        return RecursionHelper.compile(element);
      }
    };
  });