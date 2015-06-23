'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.USERROLES
 * @description
 * # USERROLES
 * Constant in the spGuiApp.
 */
angular.module('spGuiApp')
  .constant('USER_ROLES', {
    all: '*',
    admin: 'admin',
    editor: 'editor',
    guest: 'guest'
  });
