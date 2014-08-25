'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.spTalker
 * @description
 * # spTalker
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
.factory('spTalker', ['$resource', '$http', 'notificationService', '$filter', '$rootScope', function ($resource, $http, notificationService, $filter, $rootScope) {
  var apiUrl = '/api', factory = {
    activeModel: {},
    activeSPSpec: {},
    models: [],
    users: [],
    operations: [],
    items: [],
    things: [],
    thingsAsStrings: [],
    item : $resource(apiUrl + '/models/:model/items/:id', { model: '@model', id: '@id'}),
    model: $resource(apiUrl + '/models/:model', { model: '@model' }),
    user: $resource(apiUrl + '/users', {}),
    operation: $resource(apiUrl + '/models/:model/operations', { model: '@model' }, {saveArray: {method: 'POST', isArray: true}}),
    thing: $resource(apiUrl + '/models/:model/things/:thing', { model: '@model', thing: '@thing' })
  };

  if(sessionStorage.activeModel) {
    factory.activeModel = JSON.parse(sessionStorage.activeModel);
  }

  factory.getItemById = function(id) {
    var result = $.grep(factory.items, function(e){ return e.id === id; });
    if (result.length == 0) {
      var error = 'Could not find an item with id ' + id + '. The requested action has most likely been aborted.';
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

  var filterOutThings = function() {
    while(factory.things.length > 0) {
      factory.things.pop();
    }
    factory.items.forEach(function(item) {
      if(item.isa === 'Thing') {
        factory.things.push(item)
      }
    });
  };

  var listThingsAsStrings = function() {
    while(factory.thingsAsStrings.length > 0) {
      factory.thingsAsStrings.pop();
    }
    factory.things.forEach(function(thing) {
      factory.thingsAsStrings.push(thing.name);
    });

  };

  var updateItemLists = function() {
    filterOutThings();
    listThingsAsStrings();
  };

  factory.loadAll = function() {
    factory.loadModels();
    factory.item.query({model: factory.activeModel.model}, function(data) {
      angular.copy(data, factory.items);
      updateItemLists();
      factory.activeSPSpec = factory.getItemById(factory.activeModel.attributes.activeSPSpec);
      $rootScope.$broadcast('itemsQueried');
    });
  };

  if(Object.keys(factory.activeModel).length > 0) {
    factory.loadAll();
  }

  factory.saveItems = function(items, notifySuccess) {
    var success = true;
    if(items.length === 0) {
      notificationService.error('No items supplied to save.');
      return false;
    }
    if(Object.keys(factory.activeModel).length === 0) {
      notificationService.error('No active model chosen.');
      return false;
    }
    $http({method: 'POST', url: 'api/models/' + factory.activeModel.model + '/items', data: items}).
      success(function(data, status, headers, config) {
        if(notifySuccess) {
          notificationService.success(items.length + ' items were successfully saved.');
        }
        updateItemLists();
      }).
      error(function(data, status, headers, config) {
        console.log(data);
        notificationService.error('Items save failed. Please see console log for details.');
        success = false;
      });
    return success;
  };

  factory.saveItem = function(item) {
    var success = true;
    if(Object.keys(factory.activeModel).length === 0) {
      notificationService.error('No active model chosen.');
      return
    }
    if(typeof item === 'undefined' || Object.keys(item).length === 0) {
      notificationService.error('No item chosen.');
      return
    }
    item.$save(
      {model: factory.activeModel.model, id: item.id},
      function (data, headers) {
        notificationService.success(item.isa + ' \"' + item.name + '\" was successfully saved');
        updateItemLists();
        angular.copy(data, item);
      },
      function (error) {
        notificationService.error(item.isa + ' ' + item.name + ' could not be saved.');
        console.log(error);
        success = false;
      }
    );
    return success;
  };

  factory.reReadFromServer = function(item) {
    item.$get({model: factory.activeModel.model}, function(data) {
      angular.copy(data, item);
    });
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