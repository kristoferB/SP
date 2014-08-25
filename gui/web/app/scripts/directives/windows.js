'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:window
 * @description
 * # window
 */
angular.module('spGuiApp')
  .directive('windows', function () {
    return {
      templateUrl: 'views/windows.html',
      restrict: 'E',
      replace: true,
      scope: {
        windows: '=windowArray',
        active: '='
      },
      link: function postLink(scope, element, attrs) {
        var noOfOpenedWindows = 0;

        scope.addWindow = function(type, wStorage) {
          noOfOpenedWindows++;
          scope.windows.push({type: type, width: 2, height: 'large', name: type, id: type + noOfOpenedWindows, storage: wStorage});
        };

        scope.$on("newSopWindow", function() {
          if(scope.active) {
            scope.addWindow('sopMaker');
          }
        });
        scope.$on("newItemListWindow", function() {
          if(scope.active) {
            scope.addWindow('itemList');
          }
        });

        scope.closeWindow = function(window) {
          var index = scope.windows.indexOf(window);
          scope.windows.splice(index, 1);
        };

        scope.changeWindowWidth = function(window, increase) {
          if(increase && window.width !== 4){
            window.width++;
          } else if(!increase && window.width !== 1) {
            window.width--;
          }
        };

        scope.toggleWindowHeight = function(window) {
          if(window.height === 'small'){
            window.height = 'large';
          } else {
            window.height = 'small';
          }
        };

        scope.sortableOptions = {
          handle: '.draggable'
        };

      }
    };
  });
