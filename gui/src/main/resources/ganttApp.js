'use strict';

var app = angular.module('ganttApp', ['gantt']);

app.controller('ganttCtrl', ['$scope', function ($scope) {
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
}]);

app.component("ganttComponent", {
  template : `
      <h1>ng1 component</h1>
      <div ng-controller="ganttCtrl">
        <div gantt data="data">
          <gantt-tree></gantt-tree>
        </div>
      </div>
    `
});

function bootstrapGantt(element) {
  angular.bootstrap(element, ['ganttApp']);
  //var ganttComponent = document.createElement("gantt-component");
  var ganttComponent = angular.element('gantt-component');
  angular.element(document.body).append(ganttComponent);
  //angular.bootstrap(element, ['ganttComponent']);
}
