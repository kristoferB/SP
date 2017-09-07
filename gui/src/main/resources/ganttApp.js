function SPGantt(element) {

  'use strict';

  var facadedObject = {};

  var app = angular.module('ganttApp', ['gantt', 'gantt.tooltips']);

  function ganttCtrl($scope) {
      facadedObject.setData = function (rows) {
        $scope.data = rows;
        $scope.$apply();
      };
      facadedObject.addSomeRow = function() {
        $scope.addSomeRow();
        $scope.$apply();
      };
      facadedObject.addRow = function(row) {
        $scope.data.push(row);
        $scope.$apply();
      };
  }

  app.component("ganttComponent", {
    template: `
        <h1>ng1 component</h1>
          <!--
          <div gantt data="data" headers="['minute']" headers-formats="{ minute: 'mm:ss', second: 'ss' }" view-scale="'20 second'" column-width="50">
          -->
          <div gantt data="data">
            <!-- TODO need to fix some dependency stuff if we want this
            <gantt-tree></gantt-tree>
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
