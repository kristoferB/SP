'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
.directive('oplist', [ function () {

  return {
    template: '<table class="table">' +
              '<tr> ' +
              '</table>',
    restrict: 'E',
    scope: {},
    link: function postLink(scope, element, attrs) {



    }
  };
}]);
