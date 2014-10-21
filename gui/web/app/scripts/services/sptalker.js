'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.spTalker
 * @description
 * # spTalker
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
.factory('spTalker', ['$rootScope', '$resource', '$http', 'notificationService', '$timeout', function ($rootScope, $resource, $http, notificationService, $timeout) {
  var apiUrl = '/api',
    factory = {
      activeModel: {},
      models: {},
      users: [],
      operations: [],
      items: {},
      itemsRead: false,
      spSpecs: {},
      things: {},
      thingsAndOpsByName: {}
    };

  factory.getItemById = function(id) {
    return factory.items[id];
  };

  factory.getItemsByIds = function(ids) {
    var items = {};
    if(typeof ids === 'Array' || ids instanceof Array) {
      ids.forEach(function(id) {
        handleID(id);
      })
    } else {
      ids.forEach(function(value, id) {
        handleID(id);
      })
    }

    function handleID(id) {
      var item = factory.getItemById(id);
      if(item) {
        items[id] = item;
      }
    }

    return items;
  };

  factory.getItemName = function(id) {
    var item =  factory.items[id];
    if(item) {
      return item.name
    }
    var model = factory.models[id];
    if(model) {
      return model.name
    }
    return 'no name';
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

  factory.findRelations = function(ops, initState, groups, goalState) {
    return $http({
      method: 'POST',
      url: 'api/services/Relations',
      data: {
        model: factory.activeModel.model,
        operations: ops,
        initstate: initState,
        groups: groups,
        goal: goalState
      }})
  };

  factory.getSOP = function(ops, base) {
    return $http({
      method: 'POST',
      url: 'api/services/SOPMaker',
      data: {
        model: factory.activeModel.model,
        operations: ops,
        base: base
      }})
  };

  factory.createModel = function(name, successHandler) {
    var newModel = {
      name: name,
      attributes: {
        attributeTags: {},
        conditionGroups: []
      }
    };
    $http.post(apiUrl + '/models', newModel).
      success(function(model) {
        notificationService.success('A new model \"' + model.name + '\" was successfully created');
        factory.models[model.model] = model;
        if(successHandler) {
          successHandler(model);
        }
      }).
      error(function() {
        notificationService.error('The model creation failed.');
      });
  };

  function loadModels() {
    $http.get(apiUrl + '/models').
      success(function(models) {
        models.forEach(function(model) {
          factory.models[model.model] = model;
        });
      });
  }
  loadModels();

  factory.loadModel = function(id) {
    $http.get(apiUrl + '/models/' + id).
      success(function(model) {
        factory.activeModel = model;
        factory.loadAll();
      });
  };

  if(sessionStorage.activeModel) {
    factory.activeModel = { loading: 'please wait' };
    var model = angular.fromJson(sessionStorage.activeModel);
    factory.loadModel(model.model);
  }

  factory.saveModel = function(model, successHandler) {
    $http.post(apiUrl + '/models/' + model.model, model).
      success(function() {
        if(successHandler) {
          successHandler();
        }
      }).
      error(function(error) {
        console.log(error);
        notificationService.error('An error occurred during save of the active model. Please see your browser console for details.');
      })
  };

  factory.loadItems = function() {
    $http.get(apiUrl + '/models/' + factory.activeModel.model + '/items').
      success(function(items) {
        Object.keys(factory.items).forEach(function(id) {
          delete factory.items[id];
        });
        items.forEach(function(item) {
          factory.items[item.id] = item;
        });
        updateItemLists();
        factory.itemsRead = true;
        $rootScope.$broadcast('itemsQueried');
      });
  };

  function updateItemLists() {
    filterOutItems();
  }

  factory.loadAll = function() {
    loadModels();
    factory.loadItems();
  };

  function emptyMaps(mapsToEmpty) {
    for(var i = 0; i < mapsToEmpty.length; i++) {
      for(var itemID in mapsToEmpty[i]) {
        if (mapsToEmpty[i].hasOwnProperty(itemID)) {
          delete mapsToEmpty[i][itemID];
        }
      }
    }
  }

  function filterOutItems() {
    emptyMaps([factory.things, factory.thingsAndOpsByName, factory.spSpecs]);
    for(var id in factory.items) {
      if (factory.items.hasOwnProperty(id)) {
        if(factory.items[id].isa === 'Thing' || factory.items[id].isa === 'Operation') {
          factory.thingsAndOpsByName[factory.items[id].name.toLowerCase()] = factory.items[id];
          if(factory.items[id].isa === 'Thing') {
            factory.things[id] = factory.items[id];
          }
        } else if(factory.items[id].isa === 'SPSpec') {
          factory.spSpecs[id] = factory.items[id];
        }
      }
    }
  }

  factory.saveItems = function(items, notifySuccess, successHandler) {
    var success = true;
    if(items instanceof Array) {
      if(items.length === 0) {
        notificationService.error('No items supplied to save.');
        return false;
      }
    }
    if(Object.keys(factory.activeModel).length === 0) {
      notificationService.error('No active model chosen.');
      return false;
    }
    $http({method: 'POST', url: 'api/models/' + factory.activeModel.model + '/items', data: items})
      .success(function(savedItems) {
        if(notifySuccess) {
          if(savedItems.length === 0) {
            notificationService.info('No changes found to save.');
          } else if(savedItems.length === 1) {
            notificationService.success(savedItems[0].isa + ' \"' + savedItems[0].name + '\" was successfully saved.');
          } else {
            notificationService.success(savedItems.length + ' items were successfully saved.');
          }
        }
        updateItemLists();
        if(successHandler) {
          successHandler(savedItems);
        }
      })
      .error(function(data) {
        console.log(data);
        notificationService.error('Items save failed. Please see console log for details.');
        success = false;
      });
    return success;
  };

  factory.saveItem = function(item, notifySuccess, successHandler) {
    var items = [];
    items.push(item);
    factory.saveItems(items, notifySuccess, successHandler);
  };

  factory.svKindChange = function(sv, newItem) {
    var kind = sv.kind;
    delete sv.domain;
    delete sv.range;
    delete sv.boolean;
    delete sv.init;
    delete sv.goal;

    function addSVKindAttributes() {
      if(kind === 'domain') {
        sv[kind] = ['home', 'flexlink'];
        sv.init = 'home';
        sv.goal = 'flexlink';
      } else if(kind === 'range') {
        sv[kind] = {
          start: 0,
          end: 2,
          step: 1
        };
        sv.init = 0;
        sv.goal = 2
      } else if(kind === 'boolean') {
        sv[kind] = true;
        sv.init = false;
        sv.goal = true;
      }
    }

    if(newItem) {
      addSVKindAttributes();
    } else {
      $timeout(function() {
        addSVKindAttributes();
      });
    }

  };

  factory.createItem = function(type, successHandler, readyMadeItem, errorHandler, parent) {
    var newItem;
    if(typeof readyMadeItem === 'undefined' || !readyMadeItem) {
      newItem = {
        isa : type,
        name : type + Math.floor(Math.random()*1000),
        attributes : {
          children: []
        }
      };
      if(parent) {
        newItem.attributes.parent = parent.id;
      }
      if(type === 'Operation') {
        newItem.conditions = [{guard: {isa:'EQ', right: true, left: true}, action: [], attributes: {kind: 'pre', group: ''}}];
      } else if(type === 'Thing') {
        newItem.attributes.stateVariable =  {
          kind: 'range'
        };
        factory.svKindChange(newItem.attributes.stateVariable, true);
      } else if(type === 'SOPSpec') {
        newItem.sop = [{
          isa: 'Sequence',
          sop: []
        }];
      }
    } else {
      newItem = readyMadeItem;
    }

    $http.post(apiUrl + '/models/' + factory.activeModel.model + '/items', newItem).
      success(function(createdItem) {
        factory.items[createdItem.id] = createdItem;
        notificationService.success('A new ' + createdItem.isa + ' with name ' + createdItem.name + ' was successfully created.');
        updateItemLists();
        if(successHandler) {
          successHandler(createdItem);
        } else {
          $rootScope.$broadcast('itemsQueried');
        }
      }).
      error(function(error) {
        console.log(error);
        notificationService.error('Creation of ' + newItem.isa + ' failed. Check your browser\'s console for details.');
        if(errorHandler) {
          errorHandler(error);
        }
      });
  };

  factory.deleteItem = function(itemToDelete, notifySuccess) {
    var success = true;

    $http.delete(apiUrl + '/models/' + factory.activeModel.model + '/items/' + itemToDelete.id).
      success(function(deletedItem) {
        if(notifySuccess) {
          notificationService.success(deletedItem.isa + ' ' + deletedItem.name + ' was successfully deleted.');
        }
        factory.loadAll();
      }).
      error(function(error) {
        console.log(error);
        notificationService.error(itemToDelete.isa + ' ' + itemToDelete.name + ' could not be deleted from the server. Check your browser\'s error console for details.');
        success = false;
      });

    return success;
  };

  factory.reReadFromServer = function(item) {
    $http.get(apiUrl + '/models/' + factory.activeModel.model + '/items/' + item.id).
      success(function(readItem) {
        factory.items[readItem.id] = readItem;
        $rootScope.$broadcast('itemsQueried');
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