(function() {
    'use strict';

    var app = angular.module('ganttApp',['gantt']);

    app.controller('gantCtrl', ['$scope', function ($scope) {
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
})();

