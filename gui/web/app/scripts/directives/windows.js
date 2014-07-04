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
      scope: {
        windows: '=windowArray'
      },
      link: function postLink(scope, element, attrs) {
        scope.addWindow = function(type) {
          scope.windows.push({type: type, width: 'small', height: 'small', name: type, windowStorage: 'empty'});
        };

        scope.$on("newSopWindow", function() {
          scope.addWindow('sop');
        });
        scope.$on("newItemListWindow", function() {
          scope.addWindow('itemList');
        });
        scope.$on("newRestTestWindow", function() {
          scope.addWindow('restTest');
        });

        scope.closeWindow = function(window) {
          var index = scope.windows.indexOf(window);
          scope.windows.splice(index, 1);
        };

        scope.toggleWindowWidth = function(window) {
          if(window.width === 'small'){
            window.width = 'large';
          } else {
            window.width = 'small';
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
