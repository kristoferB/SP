'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.Windows
 * @description
 * # Windows
 * Service in the spGuiApp.
 */
angular.module('spGuiApp')
  .service('WindowService', function WindowService() {
    return {modelWindows: [], runtimeWindows: []};
  });
