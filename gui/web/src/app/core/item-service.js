(function () {
    'use strict';

    angular
        .module('app.core')
        .factory('itemService', itemService);

    itemService.$inject = ['logger', 'restService', '$rootScope', 'eventService', 'modelService'];
    /* @ngInject */
    function itemService(logger, restService, $rootScope, eventService, modelService) {
        var service = {
            items: [],
            itemsFetched: false,
            selected: [],
            createItem: createItem,
            deleteItem: deleteItem,
            saveItem: saveItem,
            getItem: getItem
        };

        activate();

        return service;

        function activate() {
            listenToItemEvents();
            if(modelService.activeModel !== null) {
                getAllItems(modelService.activeModel.id);
            }
            $rootScope.$on('modelChanged', function(event, model) {
                getAllItems(model.id);
            });
        }

        function getAllItems(modelID) {
            service.items.splice(0, service.items.length);
            return restService.getItems(modelID).then(function(data) {
                service.items.push.apply(service.items, data);
                logger.info('Item Service: Loaded ' + service.items.length + ' items through REST.');
                service.itemsFetched = true;
                $rootScope.$broadcast('itemsFetch', service.items);
            });
        }

        function getItem(id) {
            var index = _.findIndex(service.items, {id: id});
            if (index === -1) {
                return null
            } else {
                return service.items[index];
            }
        }

        function createItem(name, isa) {
            var newItem = {
                name: name,
                isa: isa
            };
            if (modelService.activeModel === null) {
                logger.error('Item Service: Failed to create item, no active model set.');
            } else {
                restService.postItems(newItem, modelService.activeModel.id);
            }
        }

        function saveItem(item) {
            if (modelService.activeModel === null) {
                logger.error('Item Service: Failed to save item, no active model set.');
            } else {
                restService.postItem(item, modelService.activeModel.id);
            }
        }

        function deleteItem(itemID) {
            if (modelService.activeModel === null) {
                logger.error('Item Service: Failed to request item deletion, no active model set.')
            }
            restService.deleteItem(modelService.activeModel.id, itemID);
        }

        function listenToItemEvents() {
            eventService.addListener('ModelDiff', onItemEvent);

            function onItemEvent(data) {
                logger.info('Item Service: Handler received an event.');
                    for (var i = 0; i < data.updatedItems.length; i++) {
                        var remoteItem = data.updatedItems[i];
                        var existingItem = getItem(remoteItem.id);
                        if (existingItem === null) { // item not found => create
                            service.items.push(remoteItem);
                            $rootScope.$broadcast('itemCreation', remoteItem);
                            logger.info('Item Service: Added an item with name ' + remoteItem.name + '.');
                        } else { // item found => update
                            var oldName = existingItem.name;
                            angular.extend(existingItem, remoteItem);
                            $rootScope.$broadcast('itemUpdate', existingItem);
                            logger.info('Item Service: Updated item ' + oldName + '.');
                        }
                    }
                    for (var j = 0; j < data.deletedItems.length; j++) {
                        var remoteItem = data.deletedItems[j];
                        var localItem = getItem(remoteItem.id);
                        if (localItem === null) {
                            logger.error('Item Service: Could not find an item with id ' + localItem.id + ' to delete.');
                        } else {
                            _.remove(service.items, localItem);
                            $rootScope.$broadcast('itemDeletion', remoteItem);
                            logger.info('Item Service: Removed item ' + localItem.name + '.');
                        }
                    }

            }
        }

    }
})();
