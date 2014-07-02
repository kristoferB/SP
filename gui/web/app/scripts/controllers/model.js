'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:ModelCtrl
 * @description
 * # ModelCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
.controller('ModelCtrl', function ($rootScope, $scope, WindowService) {

$scope.windows = WindowService.modelWindows;

});