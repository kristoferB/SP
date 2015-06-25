'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.AUTHEVENTS
 * @description
 * # AUTHEVENTS
 * Constant in the spGuiApp.
 */
angular.module('spGuiApp')
  .constant('AUTH_EVENTS', {
    loginSuccess: 'auth-login-success',
    loginFailed: 'auth-login-failed',
    logoutSuccess: 'auth-logout-success',
    sessionTimeout: 'auth-session-timeout',
    notAuthenticated: 'auth-not-authenticated',
    notAuthorized: 'auth-not-authorized'
  });