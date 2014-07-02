'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:ModelCtrl
 * @description
 * # ModelCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
.controller('ModelCtrl', function ($rootScope, $scope) {

$scope.windows = [];

    $scope.addWindow = function(type) {
      $scope.windows.push({type: type, width: 'small', height: 'small', name: type});
    };

});