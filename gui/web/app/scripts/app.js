'use strict';

/**
 * @ngdoc overview
 * @name spGuiApp
 * @description
 * # spGuiApp
 * hej
 * Main module of the application.
 */
angular
  .module('spGuiApp', [
    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch',
    'ngDragDrop'
  ])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })
      .when('/model', {
        templateUrl: 'views/model.html',
        controller: 'ModelCtrl'
      })
      .when('/runtime', {
        templateUrl: 'views/runtime.html',
        controller: 'RuntimeCtrl'
      })
      .otherwise({
        redirectTo: '/'
      });
  });
