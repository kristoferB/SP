'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('MainCtrl', function ($scope) {
    $scope.awesomeThings = [
      'HTML5 Boilerplate',
      'AngularJS',
      'Karma'
    ];
  });
