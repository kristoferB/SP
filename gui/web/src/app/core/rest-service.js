(function () {
    'use strict';

    angular
        .module('app.core')
        .factory('restService', restService);

    restService.$inject = ['$http', '$q', 'logger', 'API'];
    /* @ngInject */
    function restService($http, $q, logger, API) {
        var service = {
            getModels: getModels,
            getItems: getItems,
            getModelDiff: getModelDiff,
            revertModel: revertModel,
            getRuntimeKinds: getRuntimeKinds,
            getRuntimeInstances: getRuntimeInstances,
            stopRuntimeInstance: stopRuntimeInstance,
            getRegisteredServices: getRegisteredServices,
            postToModelHandler: postToModelHandler,
            postToModel: postToModel,
            postItem: postItem,
            postItems: postItems,
            postToRuntimeHandler: postToRuntimeHandler,
            postToRuntimeInstance: postToRuntimeInstance,
            postToServiceHandler: postToServiceHandler,
            postToServiceInstance: postToServiceInstance,
            deleteModel: deleteModel,
            deleteItem: deleteItem
        };

        return service;

        function getModels() { return getStuff(API.models, 'models'); }
        function getItems(modelID) { return getStuff(API.items(modelID), 'items'); }
        function getModelDiff(modelID, version) { return getStuff(API.modelDiff(modelID,version), 'model diff'); }
        function revertModel(modelID, version) { return getStuff(API.revertModel(modelID, version), 'revert model'); }
        function getRuntimeKinds() { return getStuff(API.runtimeKinds, 'runtime kinds'); }
        function getRuntimeInstances() { return getStuff(API.runtimeHandler, 'runtime instances'); }
        function stopRuntimeInstance(runtimeID) { return getStuff(API.stopRuntimeInstance(runtimeID), 'runtime stop'); }
        function getRegisteredServices() { return getStuff(API.serviceHandler, 'registered services'); }

        function getStuff(restURL, itemKind) {

            return $http.get(restURL)
                .then(success)
                .catch(fail);

            function success(response) {
                logger.info('REST Service: Successfully fetched ' + response.data.length + ' ' + itemKind + ' through REST.');
                return response.data;
            }

            function fail(error) {
                var msg = 'REST Service: Query for ' + itemKind  + ' failed. ' + error.data;
                logger.error(msg);
                return $q.reject(msg);
            }
        }

        function postToModelHandler(data) { return postStuff(API.models, 'model handler', data); }
        function postToModel(modelID, data) { return postStuff(API.model(modelID), 'model actor', data); }
        function postItem(item, modelID) { return postStuff(API.item(modelID, item.id), 'model actor', item); }
        function postItems(data, modelID) { return postStuff(API.items(modelID), 'model actor', data); }
        function postToRuntimeHandler(data) { return postStuff(API.runtimeHandler, 'runtime handler', data); }
        function postToRuntimeInstance(data, runtimeID) {
            return postStuff(API.runtimeInstance(runtimeID), 'runtime instance', data);
        }
        function postToServiceHandler(data) { return postStuff(API.serviceHandler, 'service handler', data); }
        function postToServiceInstance(data, serviceID) {
            return postStuff(API.serviceInstance(serviceID), 'service instance', data);
        }

        function postStuff(restURL, receiver, data) {
            return $http.post(restURL, data)
                .then(success)
                .catch(fail);

            function success(response) {
                logger.info('REST Service: Successfully posted data to ' + receiver + '.');
                return response.data;
            }

            function fail(error) {
                var msg = 'REST Service: Post of data to ' + receiver  + ' failed. ' + error.data;
                logger.error(msg);
                return $q.reject(msg);
            }
        }

        function deleteModel(modelID) { return deleteStuff(API.model(modelID), 'model'); }
        function deleteItem(modelID, itemID) { return deleteStuff(API.item(modelID, itemID), 'item'); }

        function deleteStuff(restURL, itemKind) {

            return $http.delete(restURL)
                .then(success)
                .catch(fail);

            function success(response) {
                logger.info('REST Service: Successfully sent deletion/stop request for ' + itemKind + '.');
                return response.data;
            }

            function fail(error) {
                var msg = 'REST Service: Deletion of ' + itemKind  + ' failed. ' + error.data;
                logger.error(msg);
                return $q.reject(msg);
            }
        }
    }
})();
