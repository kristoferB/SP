angular.module('ngrepeatSelect', [])
 .controller('ExampleController', ['$scope', function($scope) {
   $scope.data = {
    repeatSelect: null,
    availableOptions: [
      {resource: 'Elevator 1', 
      	idlist: ['135, 0, 0, true', '135, 0, 1, true'],
      	action: ['up','down']},
      {resource: 'Elevator 2',
      	idlist: ['140, 0, 0, true', '140, 0, 1, true'],
      	action: ['up','down']},
      {resource: 'Flexlink',
      	idlist: ['139, 0, SAKNAS, true', '139, 0, SAKNAS, true'],
      	action: ['start', 'stop']},
      {resource: 'Robot R4',
      	idlist: ['128, 0, 2, true', '128, 0, 3, true'],
      	action: ['home', 'dodge']},
      {resource: 'Robot R5',
      	idlist: ['132, 0, 2, true', '132, 0, 3, true'],
      	action: ['home', 'dodge']},
      {resource: 'Robot R2',
      	idlist: [''],
      	action: ['']},

    ],
   };
}]);