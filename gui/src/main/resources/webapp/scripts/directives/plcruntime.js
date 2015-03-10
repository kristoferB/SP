'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:plcRuntime
 * @description
 * # plcRuntime
 */
angular.module('spGuiApp')
  .directive('plcRuntime', function (sse) {
    return {
      templateUrl: 'views/plcruntime.html',
      restrict: 'E',
      link: function postLink(scope) {

        scope.foos = [];

        scope.addSSEListener = function() {
          sse.addSSEListener('stateChange', function (e) {
            console.log(e);
            scope.foos.push({value: e.data});
          });
        };

      }
    };
  });