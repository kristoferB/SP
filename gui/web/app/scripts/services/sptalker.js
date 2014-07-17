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
    model: $resource(apiUrl + '/models/', {}),
    user: $resource(apiUrl + '/users', {}),
    operation: $resource(apiUrl + '/models/:model/operations/:op', { model: '@model', op: '@op' }),
    thing: $resource(apiUrl + '/models/:model/things/:thing', { model: '@model', thing: '@thing' }),
    item: $resource(apiUrl + '/models/:model/items', { model: '@model' })
  };

  factory.loadAll = function() {
    factory.models = factory.model.query();
  };

  //factory.loadAll();

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

    $http({method: 'POST', url: 'api/users', data: {userName: 'test', password: 'demo', name: 'Ben'}}).
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
        });



  return factory;

}]);