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
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch',
    'ui.sortable',
    'jlareau.pnotify',
    'ui.bootstrap'
  ])
  .config(function ($routeProvider, USER_ROLES) {
    $routeProvider
      /*.when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })*/
      .when('/model', {
        templateUrl: 'views/model.html',
        controller: 'ModelCtrl',
        data: [USER_ROLES.admin, USER_ROLES.editor]
      })
      .when('/runtime', {
        templateUrl: 'views/runtime.html',
        controller: 'RuntimeCtrl',
        data: [USER_ROLES.admin, USER_ROLES.editor]
      })
      .otherwise({
        redirectTo: '/model'
      });
  })
  .config(function ($httpProvider) {
    $httpProvider.interceptors.push([
      '$injector',
      function ($injector) {
        return $injector.get('AuthInterceptor');
      }
    ]);
  })
  .run(function ($rootScope, AUTH_EVENTS, AuthService) {
    $rootScope.$on('$routeChangeStart', function (event, next, current) {
      var authorizedRoles = next.data;
      console.log(authorizedRoles);
      if (!AuthService.isAuthorized(authorizedRoles)) {
        console.log("Trying to prevent default");
        event.preventDefault();
        if (AuthService.isAuthenticated()) {
          // user is not allowed
          console.log('The current user is not allowed to access this page.');
          $rootScope.$broadcast(AUTH_EVENTS.notAuthorized);
        } else {
          // user is not logged in
          console.log('No user is logged in.');
          $rootScope.$broadcast(AUTH_EVENTS.notAuthenticated);
        }
      }
      $rootScope.vars.isLoginPage = false;
    });
  })
  .run(function($rootScope, $location) {
    $rootScope.location = $location;
  });
