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
      scope: {
        ws: '=windowStorage'
      },
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

        scope.executeOp = function(idOfOpToExecute) {
          var currentStates = [];
          Object.keys(scope.currentStates).forEach(function(id) {
            currentStates.push({id: id, value: scope.currentStates[id]});
          });

            if(typeof scope.ws.runtimeName === 'undefined' || scope.ws.runtimeName === '') {
              notificationService.info('You have to supply a runtime name.');
            }
            var newState = {
              model: spTalker.activeModel.model,
              state: currentStates
            };
            if(typeof idOfOpToExecute !== 'undefined' && idOfOpToExecute !== '') {
              newState.execute = idOfOpToExecute;
            }
            $http({
              method: 'POST',
              url: API_URL + '/runtimes/' + scope.ws.runtimeName,
              data: newState})
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
