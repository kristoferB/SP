'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:SiteCtrl
 * @description
 * # SitecontrollerCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
  .controller('SiteCtrl', [ function ($scope, $routeParams, $location) {

    $("[name='model-runtime-switch']").bootstrapSwitch('size', 'small');
    $("[name='model-runtime-switch']").bootstrapSwitch('offText', 'Model');
    $("[name='model-runtime-switch']").bootstrapSwitch('onText', 'Runtime');
    $("[name='model-runtime-switch']").bootstrapSwitch('offColor', 'default');
    $("[name='model-runtime-switch']").bootstrapSwitch('onColor', 'default');
    $("[name='model-runtime-switch']").bootstrapSwitch('state', false);

    $("[name='model-runtime-switch']").on('switchChange.bootstrapSwitch', function(event, state) {
      if (state === true) {
        $location.path('/runtime');
      } else {
        $location.path('/model');
      }
      $location.replace();
      $scope.$apply();
    });

  }]);
