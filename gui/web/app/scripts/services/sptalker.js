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
  var apiUrl = '/api', factory = {
    activeModel: null,
    models: [],
    model: $resource(apiUrl + '/models', {}),
    operation: $resource(apiUrl + '/models/:model/operations/:op', { model: '@model', op: '@op' }),
    thing: $resource(apiUrl + '/models/:model/things/:thing', { model: '@model', thing: '@thing' }),
    item: $resource(apiUrl + '/models/:model/items', { model: '@model' })
  };

  factory.loadAll = function() {
    factory.models = factory.model.query();
  };

  factory.loadAll();

  return factory;

}]);