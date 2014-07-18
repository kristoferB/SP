'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.spTalker
 * @description
 * # spTalker
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
.factory('spTalker', ['$resource', '$http', function ($resource, $http) {
  var apiUrl = '/api', factory = {
    activeModel: null,
    models: [],
    users: [],
    operations: [],
    items: [],
    model: $resource(apiUrl + '/models/', {}),
    user: $resource(apiUrl + '/users', {}),
    operation: $resource(apiUrl + '/models/:model/operations', { model: '@model' }, {saveArray: {method: 'POST', isArray: true}}),
    thing: $resource(apiUrl + '/models/:model/things/:thing', { model: '@model', thing: '@thing' }),
    item: $resource(apiUrl + '/models/:model/items', { model: '@model' }, {saveArray: {method: 'POST', isArray: true}})
  };

  factory.loadModels = function() {
    factory.models = factory.model.query();
  };

  factory.loadModels();

  factory.loadAll = function() {
    factory.loadModels();
    factory.items = factory.item.query({model: factory.activeModel.model});
  };

  /*$http.defaults.headers.common['Authorization'] = 'Basic ' + window.btoa('admin' + ':' + 'pass');
  $http({method: 'GET', url: 'api/secured'}).
    success(function(data, status, headers, config) {
        console.log(data);
        // this callback will be called asynchronously
        // when the response is available
    }).
    error(function(data, status, headers, config) {
        console.log(data);
        // called asynchronously if an error occurs
        // or server returns response with an error status.
    });*/

    /*$http({method: 'POST', url: 'api/users', data: {userName: 'test', password: 'demo', name: 'Ben'}}).
        success(function(data, status, headers, config) {
            console.log(data);
            // this callback will be called asynchronously
            // when the response is available
        }).
        error(function(data, status, headers, config) {
            console.log(data);
            // called asynchronously if an error occurs
            // or server returns response with an error status.
        });

    $http({method: 'GET', url: 'api/users'}).
        success(function(data, status, headers, config) {
            console.log(data);
            // this callback will be called asynchronously
            // when the response is available
        }).
        error(function(data, status, headers, config) {
            console.log(data);
            // called asynchronously if an error occurs
            // or server returns response with an error status.
        });*/



  return factory;

}]);