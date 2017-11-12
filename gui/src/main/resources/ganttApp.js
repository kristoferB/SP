function SPGantt(element, options) {

  'use strict';

  var facadedObject = {};

  var app = angular.module('ganttApp', ['gantt', 'gantt.tooltips', 'gantt.table']);

  function ganttCtrl($scope) {
      facadedObject.setData = function(rows) {
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

      $scope.headers = options.headers;

      $scope.headersFormats = {
        hour: 'H:mm',
        minute: 'H:mm',
        second: 'ss'
      };
      /*
       * TODO this would make time-label appear every 20 minutes
       * (as appropriate in erica I think), maybe not available unless
       * we get the latest version of angular-gantt
      var twentyMinutes = moment.duration({'minutes': 20});
      $scope.headersScales = {
        minute: twentyMinutes
      };
      */

      $scope.viewScale = options.viewScale;
  }

  app.component("ganttComponent", {
    template: `
          <div gantt data="data" headers="headers" headers-formats="headersFormats" headers-scales="headersScales" view-scale="viewScale" column-width="50">
            <!-- TODO need to fix some dependency stuff if we want this
            <gantt-tree></gantt-tree>
            -->
            <gantt-tooltips date-format="'H:mm:ss'" delay="100"></gantt-tooltips>
            <gantt-table headers="{'model.name': ''}"></gantt-table>
          </div>
      `
      ,
    controller: ganttCtrl
  });

  angular.bootstrap(element, ['ganttApp']);

  return facadedObject;
}
