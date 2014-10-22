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
          } else if(type === 'sopMaker') {
            wStorage.editable = true;
          } else if(type === 'identifyRelations') {
            width = 1;
          } else if(type === 'ganttSchedule') {
            height = 'small';
          }

          scope.windows.push({type: type, width: width, height: height, name: type, id: type + noOfOpenedWindows, storage: wStorage});
          sessionStorage.noOfOpenedWindows = angular.toJson(noOfOpenedWindows);
        };

        scope.$on("newSopMakerWindow", function() {
          if(scope.active) {
            scope.addWindow('sopMaker', {});
          }
        });
        scope.$on("newItemListWindow", function() {
          if(scope.active) {
            scope.addWindow('itemList');
          }
        });
        scope.$on("newGanttScheduleWindow", function() {
          if(scope.active) {
            scope.addWindow('ganttSchedule');
          }
        });
        //TODO: We need to handle algorithms in a more generic way (load from server) KB 140828
        scope.$on("identifyRelations", function() {
          if(scope.active) {
            scope.addWindow('identifyRelations');
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
