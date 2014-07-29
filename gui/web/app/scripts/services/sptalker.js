'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.spTalker
 * @description
 * # spTalker
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
.factory('spTalker', ['$resource', '$http', 'notificationService', function ($resource, $http, notificationService) {
  var apiUrl = '/api', factory = {
    activeModel: {},
    models: [],
    users: [],
    operations: [],
    items: [],
    model: $resource(apiUrl + '/models/', {}),
    user: $resource(apiUrl + '/users', {}),
    operation: $resource(apiUrl + '/models/:model/operations', { model: '@model' }, {saveArray: {method: 'POST', isArray: true}}),
    thing: $resource(apiUrl + '/models/:model/things/:thing', { model: '@model', thing: '@thing' })
  };

  factory.item = $resource(apiUrl + '/models/:model/items/:id', { model: '@model', id: '@id'});

  factory.getItemById = function(id) {
    var result = $.grep(factory.items, function(e){ return e.id === id; });
    if (result.length == 0) {
      var error = 'Could not find any item with id ' + id + '. The requested action has most likely been aborted.';
      console.log(error);
      notificationService.error(error);
      return {};
    } else if (result.length == 1) {
      return result[0];
    } else {
      var error2 = 'Found multiple items with id ' + id + '. The requested action has most likely been aborted.';
      console.log(error2);
      notificationService.error(error2);
      return result[0];
    }
  };

  factory.parseProposition = function(proposition) {
    return $http({method: 'POST', url: 'api/services/PropositionParser', data: {model: factory.activeModel.model, parse: proposition}})
  };

  factory.loadModels = function() {
    factory.models = factory.model.query();
  };

  factory.loadModels();

  factory.loadAll = function() {
    factory.loadModels();
    factory.items = factory.item.query({model: factory.activeModel.model});
  };

  factory.saveItem = function(item) {
    factory.save(item);
  };

  factory.save = function(item) {
    item.$save(
      {model: factory.activeModel.model, id: item.id},
      function (data, headers) {
        notificationService.success(item.isa + ' \"' + item.name + '\" was successfully saved');
      },
      function (error) {
        notificationService.error(item.isa + ' ' + item.name + ' could not be saved. ' + error.data);
        console.log(error);
        factory.reReadFromServer(item);
      }
    );
  };

  factory.reReadFromServer = function(item) {
    item.$get({model: factory.activeModel.model});
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