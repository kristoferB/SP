function addTheGantt(element) {

  'use strict';

  var facadedObject = {};

  var app = angular.module('ganttApp', ['gantt', 'gantt.tooltips']);

  function ganttCtrl($scope) {

      $scope.data = [
          {name: 'row1', tasks: [
              {name: 'task1', from: new Date(2013, 3, 30, 18, 0, 0), to: new Date(2013, 4, 10, 18, 0, 0)},
              {name: 'task2', from: new Date(2013, 4, 15, 18, 0, 0), to: new Date(2013, 4, 20, 18, 0, 0)}
            ]
          },
          {name: 'row2', tasks: [
              {name: 'task3', from: new Date(2013, 3, 30, 18, 0, 0), to: new Date(2013, 4, 12, 18, 0, 0)},
              {name: 'task4', from: new Date(2013, 4, 15, 18, 0, 0), to: new Date(2013, 4, 20, 18, 0, 0)}
            ]
          }

      ];

      $scope.addRow = function() {
        console.log("clicked addRow");
        $scope.data.push(
          {name: 'newRow', tasks: [
              {name: 'newRowTask', from: new Date(2013, 3, 30, 18, 0, 0), to: new Date(2013, 4, 12, 18, 0, 0)}
              ]
          }
        )

      };

      facadedObject.addRow = function() {
        $scope.addRow();
        $scope.$apply();
      }
  }

  app.component("ganttComponent", {
    template: `
        <h1>ng1 component</h1>
          <div gantt data="data">
            <!-- TODO need to fix some dependency stuff if we want this
            <gantt-tree enabled="true"></gantt-tree>
            -->
            <gantt-tooltips date-format="'mm:ss'" delay="100"></gantt-tooltips>
          </div>
          <button ng-click="addRow()">add row</button>
      `
      ,
    controller: ganttCtrl
  });

  angular.bootstrap(element, ['ganttApp']);

  return facadedObject;
}
