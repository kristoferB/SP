'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.spTalker
 * @description
 * # spTalker
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
.factory('spTalker', ['$resource', function ($resource) {
  var apiUrl = "/api";

  return {
    models: $resource(apiUrl + '/models/:model', { model:'@model' }),
    operations: $resource(apiUrl + '/models/:model/operations/:op', { model:'@model', op:'@op' }),
    things: $resource(apiUrl + '/models/:model/things/:thing', { model:'@model', thing:'@thing' }),
    items: $resource(apiUrl + '/models/:model/items', { model:'@model' })
  };

}]);