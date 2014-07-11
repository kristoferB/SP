'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.Session
 * @description
 * # Session
 * Service in the spGuiApp.
 */
angular.module('spGuiApp')
  .service('Session', function Session() {
    this.create = function (sessionId, userId, userRole) {
      this.id = sessionId;
      this.userId = userId;
      this.userRole = userRole;
    };
    this.destroy = function () {
      this.id = null;
      this.userId = null;
      this.userRole = null;
    };
    return this;
  });
