'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:window
 * @description
 * # window
 */
angular.module('spGuiApp')
  .directive('windows', function (tabSvc) {
    return {
      templateUrl: 'views/windows.html',
      restrict: 'E',
      replace: true,
      scope: {
        windows: '=windowArray',
        active: '='
      },
      link: function postLink(scope) {
        var noOfOpenedWindows;

        if(sessionStorage.noOfOpenedWindows) {
          noOfOpenedWindows = angular.fromJson(sessionStorage.noOfOpenedWindows);
        } else {
          noOfOpenedWindows = 0
        }

        scope.addWindow = function(type, wStorage) {
          var width = 2, height = 'large';
          noOfOpenedWindows++;
          if(type === 'sopViewer') {
            wStorage.editable = false;
            width = 1;
            height = 'small';
          } else if(type === 'sopMaker') {
            wStorage.editable = true;
          } else if(type === 'itemTree') {
            wStorage.itemTree = true;
            width = 1;
          } else if(type === 'itemList') {
            wStorage.itemTree = false;
          } else if(type === 'itemExplorer' || type === 'itemInfo') {
            width = 1;
            height = 'small';
          } else if(type === 'identifyRelations') {
            width = 1;
          } else if(type === 'schedule') {
            height = 'small';
          }

          scope.windows.push({type: type, width: width, height: height, name: type, id: type + noOfOpenedWindows, storage: wStorage});
          sessionStorage.noOfOpenedWindows = angular.toJson(noOfOpenedWindows);
        };

        scope.$on('newWindow', function() {
          if(scope.active) {
            scope.addWindow(tabSvc.typeOfNewWindow, tabSvc.dataForNewWindow);
          }
        });

        //TODO: We need to handle algorithms in a more generic way (load from server) KB 140828

        scope.closeWindow = function(window) {
          var index = scope.windows.indexOf(window);
          scope.windows.splice(index, 1);
        };

        scope.changeWindowWidth = function(window, increase) {
          if(increase && window.width !== 5){
            window.width++;
          } else if(!increase && window.width !== 0) {
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
