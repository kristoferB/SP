'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.spTalker
 * @description
 * # spTalker
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
.factory('spTalker', ['$rootScope', '$resource', '$http', 'notificationService', '$filter', function ($rootScope, $resource, $http, notificationService, $filter) {
  var apiUrl = '/api',
    dummySPSpec = {
      id: 0,
      attributes: {
        attributeTags: {}
      }
    },
    factory = {
      activeModel: {},
      activeSPSpec: dummySPSpec,
      models: [],
      users: [],
      operations: [],
      items: {},
      itemsRead: false,
      things: {},
      thingsByName: {},
      item : $resource(apiUrl + '/models/:model/items/:id', { model: '@model', id: '@id'}),
      model: $resource(apiUrl + '/models/:modelID', { modelID: '@modelID' }),
      user: $resource(apiUrl + '/users', {}),
      operation: $resource(apiUrl + '/models/:model/operations', { model: '@model' }, {saveArray: {method: 'POST', isArray: true}}),
      thing: $resource(apiUrl + '/models/:model/things/:thing', { model: '@model', thing: '@thing' })
    };

  if(sessionStorage.activeModel) {
    factory.activeModel = { loading: 'please wait' };
    var model = angular.fromJson(sessionStorage.activeModel);
    factory.model.get({modelID: model.model}, function(model) {
      factory.activeModel = model;
      factory.loadAll();
    }, function(error) {
      console.log(error);
    });
  }

  factory.getItemById = function(id) {
    return factory.items[id];
    /*var result = $.grep(factory.items, function(e){ return e.id === id; });
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
    }*/
  };

  factory.getItemName = function(id) {
    var item =  _.find(factory.items, function(item) {
      return item.id == id
    });
    return item.name
  };

  //TODO: Handle services in a general way. Retrieve possible services from server
  factory.parseProposition = function(proposition) {
    return $http({
      method: 'POST',
      url: 'api/services/PropositionParser',
      data: {
        model: factory.activeModel.model,
        parse: proposition
      }})
  };

  factory.findRelations = function(ops, initState) {
    return $http({
      method: 'POST',
      url: 'api/services/Relations',
      data: {
        model: factory.activeModel.model,
        operations: ops,
        initstate: initState
      }})
  };

  factory.loadModels = function() {
    factory.models = factory.model.query();
  };
  factory.loadModels();

  var filterOutThings = function() {
    for(var thingID in factory.things) {
      if (factory.things.hasOwnProperty(thingID)) {
        delete factory.thingsByName[factory.items[thingID].name];
        delete factory.things[thingID];
      }
    }
    for(var id in factory.items) {
      if (factory.items.hasOwnProperty(id)) {
        if (factory.items[id].isa === 'Thing') {
          factory.things[id] = factory.items[id];
          factory.thingsByName[factory.items[id].name.toLowerCase()] = factory.items[id];
        }
      }
    }
  };

  var updateItemLists = function() {
    filterOutThings();
  };

  factory.loadAll = function() {
    factory.loadModels();
    factory.item.query({model: factory.activeModel.model}, function(items) {
      //angular.copy(items, factory.items);
      items.forEach(function(item) {
        factory.items[item.id] = item;
      });
      factory.activeSPSpec = factory.getItemById(factory.activeModel.attributes.activeSPSpec);
      updateItemLists();
      $rootScope.$broadcast('itemsQueried');
      factory.itemsRead = true;
    });
  };

  factory.saveItems = function(items, notifySuccess, successHandler) {
    var success = true;
    if(items.length === 0) {
      notificationService.error('No items supplied to save.');
      return false;
    }
    if(Object.keys(factory.activeModel).length === 0) {
      notificationService.error('No active model chosen.');
      return false;
    }
    $http({method: 'POST', url: 'api/models/' + factory.activeModel.model + '/items', data: items})
      .success(function(data) {
        data.forEach( function(itemData) {
          var item = factory.getItemById(itemData.id);
          angular.copy(itemData, item);
        });
        if(notifySuccess) {
          notificationService.success(items.length + ' items were successfully saved.');
        }
        updateItemLists();
        successHandler(data);
      })
      .error(function(data) {
        console.log(data);
        notificationService.error('Items save failed. Please see console log for details.');
        success = false;
      });
    return success;
  };

  factory.saveItem = function(item, showSuccessNotifications, successHandler) {
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
      function (data) {
        if(showSuccessNotifications) {
          notificationService.success(item.isa + ' \"' + item.name + '\" was successfully saved');
        }
        /*if(data !== item) {
          angular.copy(data, item);
        }*/
        updateItemLists();
        if(successHandler) {
          successHandler(data);
        }
      },
      function (error) {
        notificationService.error(item.isa + ' ' + item.name + ' could not be saved.');
        console.log(error);
        success = false;
      }
    );
    return success;
  };

  factory.createItem = function(type, successHandler) {
    var newItem = new factory.item({
      isa : type,
      name : type + Math.floor(Math.random()*1000),
      attributes : {
        children: []
      }
    });
    if(type === 'Operation') {
      newItem.conditions = [];
    } else if(type === 'Thing') {
      newItem.stateVariables = [];
    } else if(type === 'SOPSpec') {
      newItem.sop = [{
        isa: 'Sequence',
        sop: []
      }];
    } else if(type === 'SPSpec') {
      newItem.attributes.attributeTags = {}
    }
    newItem.$save(
      {model:factory.activeModel.model},
      function(data) {
        factory.items[data.id] = data;
        notificationService.success('A new ' + data.isa + ' with name ' + data.name + ' was successfully created.');
        successHandler(data);
      },
      function(error) { console.log(error); notificationService.error('Creation of ' + newItem.isa + ' failed. Check your browser\'s console for details.'); console.log(error); }
    );
  };

  factory.deleteItem = function(itemToDelete) {
    // remove item from parent items
    for(var id in factory.items) {
      if(factory.items.hasOwnProperty(id)) {
        var index = factory.items[id].attributes.children.indexOf(itemToDelete.id);
        if (index !== -1) {
          factory.items[id].attributes.children.splice(index, 1);
          factory.saveItem(factory.items[id], false);
        }
      }
    }

    removeItemFromModel();

    function removeItemFromModel() {
      var index = factory.activeModel.attributes.children.indexOf(itemToDelete.id);
      if(index >= 0) {
        factory.activeModel.attributes.children.splice(index, 1);
        factory.activeModel.$save(
          {modelID: factory.activeModel.model},
          function() {
            removeItemFromServer();
          }
        );
      } else {
        removeItemFromServer();
      }
    }

    function removeItemFromServer() {
      itemToDelete.$delete(
        {model:factory.activeModel.model},
        function(data) {
          delete factory.items[data.id];
          notificationService.success(data.isa + ' ' + data.name + ' was successfully deleted.');
          $rootScope.$broadcast('itemsQueried');
        },
        function(error) {
          console.log(error);
          notificationService.error(itemToDelete.isa + ' ' + itemToDelete.name + ' could not be deleted from the server. Check your browser\'s error console for details.');
        }
      );
    }
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