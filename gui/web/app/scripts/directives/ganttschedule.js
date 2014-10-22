'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:ganttSchedule
 * @description
 * # ganttSchedule
 */
angular.module('spGuiApp')
  .directive('ganttSchedule', function ($timeout) {
    return {
      templateUrl: 'views/ganttschedule.html',
      restrict: 'E',
      controller: function postLink($scope) {
        $scope.ganttInitialized = false;

        $scope.currentDate = new Date(2014,9,23,11,20,0);

        $scope.addSamples = function () {
          $scope.loadTimespans(getSampleTimespans().timespan1);
          $scope.loadData(getSampleData().data1);
          $timeout(function() {
            $scope.scrollToDate($scope.currentDate);
          },0,true);
        };

        $scope.ganttInitialized = function() {
          // Start using the Gantt e.g. load data
          $scope.ganttInitialized = true;
        };

        function getSampleData() {

          return {
            "data1": [
              // Order is optional. If not specified it will be assigned automatically
              {"id": "2f85dbeb-0845-404e-934e-218bf39750c0", "description": "o1", "order": 0, "tasks": [
                // Dates can be specified as string, timestamp or javascript date object. The data attribute can be used to attach a custom object
                {"id": "f55549b5-e449-4b0c-9f4b-8b33381f7d76", "subject": "o3", "color": "#ebebeb", "from": new Date(2014,9,23,0,0,0), "to": new Date(2014,9,23,5,0,0), "data": "Can contain any custom data or object"}
              ], "data": "Can contain any custom data or object"},
              {"id": "b8d9927-cf50-48bd-a056-3554decab824", "description": "o2", "order": 1, "tasks": [
                {"id": "301d781f-1ef0-4c35-8398-478b641c0658", "subject": "o2", "color": "#ebebeb", "from": new Date(2014,9,23,3,0,0), "to": new Date(2014,9,23,11,0,0)}
              ]},
              {"id": "c65c2672-445d-4297-a7f2-30de241b3145", "description": "o3", "order": 2, "tasks": [
                {"id": "4e197e4d-02a4-490e-b920-4881c3ba8eb7", "subject": "o3", "color": "#ebebeb", "from": new Date(2014,9,23,3,0,0), "to": new Date(2014,9,23,11,0,0)}
              ]},
              {"id": "dd2e7a97-1622-4521-a807-f29960218785", "description": "o4", "order": 3, "tasks": [
                {"id": "9c17a6c8-ce8c-4426-8693-a0965ff0fe69", "subject": "o4", "color": "#ebebeb", "from": new Date(2014,9,23,15,0,0), "to": new Date(2014,9,23,23,0,0)}
              ]}
            ]};
        }

        function getSampleTimespans() {
          return {
            "timespan1": [
              {
                id: '1',
                from: new Date(2014,9,23,7,30,0),
                to: new Date(2014,9,23,14,30,0),
                subject: 'Sprint 1 Timespan',
                priority: null,
                //classes: [], //Set custom classes names to apply to the timespan.
                data: null
              }
            ]
          };
        }

      }
    };
  });
