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

        scope.resetStates = function() {
          Object.keys(scope.things).forEach(function(id) {
            scope.currentStates[id] = scope.things[id].attributes.stateVariable.init;
          });
          Object.keys(scope.ops).forEach(function(id) {
            scope.currentStates[id] = 'i';
          });
          scope.executeOp();
        };

        scope.createRuntime = function() {
          spTalker.createRuntime()
            .success(function(runtime) {
              scope.runtimeName = runtime.name;
              notificationService.success('A new runtime \"' + runtime.name + '\" was successfully created');
              scope.resetStates();
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
              scope.currentStates = response.state;
            }).
            error(function() {
              notificationService.error('The runtime state update failed.');
            });
        };
      }
    };
  });
