'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:RuntimeCtrl
 * @description
 * # RuntimeCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('RuntimeCtrl', function ($scope, WindowService) {

    $scope.windows = WindowService.runtimeWindows;
        
  });
