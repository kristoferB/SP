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
        windows: '=windowArray'
      },
      link: function postLink(scope, element, attrs) {
        var noOfOpenedWindows = 0;

        scope.addWindow = function(type, wStorage) {
          if(typeof wStorage === 'undefined') {
            wStorage = {};
          }
          noOfOpenedWindows++;
          scope.windows.push({type: type, width: 1, height: 'small', name: type, windowStorage: wStorage, id: type + noOfOpenedWindows});
        };

        scope.$on("newSopWindow", function() {
          scope.addWindow('sopMaker');
        });
        scope.$on("newItemListWindow", function() {
          scope.addWindow('itemList');
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
          /*start: function(event, ui) {
           ui.item.removeClass('sizeTransition');
           },
           stop: function(event, ui) {
           ui.item.addClass('sizeTransition');
           },*/
          handle: '.draggable'
        };

      }
    };
  });
