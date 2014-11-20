'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:simulationRuntime
 * @description
 * # simulationRuntime
 */
angular.module('spGuiApp')
  .directive('simulationRuntime', function (spTalker, notificationService) {
    return {
      templateUrl: 'views/simulationruntime.html',
      restrict: 'E',
      link: function postLink(scope) {
        scope.currentStates = {};
        scope.spTalker = spTalker;
        scope.enabled = [];

        scope.createRuntime = function() {
          spTalker.createRuntime()
            .success(function(runtime) {
              scope.runtimeName = runtime.name;
              notificationService.success('A new runtime \"' + runtime.name + '\" was successfully created');
              scope.executeOp();
            })
            .error(function() {
              notificationService.error('The runtime creation failed.');
            });
        };
        scope.createRuntime();

        scope.executeOp = function(idOfOpToExecute) {
          var currentStates = [];
          Object.keys(scope.currentStates).forEach(function(id) {
            currentStates.push({id: id, value: scope.currentStates[id]});
          });
          spTalker.updateState(scope.runtimeName, currentStates, idOfOpToExecute)
            .success(function(response) {
              if(angular.isDefined(response.error)) {
                console.log(response);
                notificationService.info(response.error);
                return;
              }
              scope.enabled = response.enabled;
              response.state.forEach(function(state) {
                scope.currentStates[state.id] = state.value;
              });
            }).
            error(function() {
              notificationService.error('The runtime state update failed.');
            });
        };
      }
    };
  });
