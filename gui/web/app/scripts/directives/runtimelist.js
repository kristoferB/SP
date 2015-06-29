'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:runtimeList
 * @description
 * # runtimeList
 */
angular.module('spGuiApp')
  .directive('runtimeList', function (API_URL, spTalker, tabSvc, $http, notificationService) {
    return {
      templateUrl: 'views/runtimelist.html',
      restrict: 'E',
      link: function postLink(scope) {

        scope.runtimes = [];
        scope.runtimeKinds = [];

        scope.getRuntimes = function() {
          $http.get(API_URL + '/runtimes').
            success(function(returnedRuntimes) {
              scope.runtimes = returnedRuntimes;
            });
        };
        scope.getRuntimes();

        scope.getRuntimeKinds = function() {
          $http.get(API_URL + '/runtimes/kinds').
            success(function(returnedRuntimeKinds) {
              scope.runtimeKinds = returnedRuntimeKinds;
            });
        };
        scope.getRuntimeKinds();

        scope.createRuntime = function(kind) {
          $http({
            method: 'POST',
            url: API_URL + '/runtimes',
            data: {
              kind: kind,
              model: spTalker.activeModel.model,
              name: kind + Math.floor(Math.random()*1000)
            }})
            .success(function(runtime) {
              scope.runtimes.push(runtime);
              notificationService.success('A new runtime \"' + runtime.name + '\" was successfully created');
            })
            .error(function() {
              notificationService.error('The runtime creation failed.');
            });
        };

        scope.openRuntime = function(runtimeKind, runtimeName) {
          var data = {
            runtimeName: runtimeName
          };
          tabSvc.newWindow(runtimeKind, data)
        };

        scope.stopRuntime = function(runtime) {
          $http({
            method: 'GET',
            url: API_URL + '/runtimes/' + runtime.name + '/stop'
          })
            .success(function() {
              scope.runtimes.splice(scope.runtimes.indexOf(runtime), 1);
              notificationService.success('Runtime \"' + runtime.name + '\" was stopped.');
            })
            .error(function(e) {
              notificationService.error(e);
            });
        }

      }
    };
  });
