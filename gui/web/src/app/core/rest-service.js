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
            getRegisteredServices: getRegisteredServices,
            postToModelHandler: postToModelHandler,
            postToModel: postToModel,
            postItem: postItem,
            postItems: postItems,
            postToServiceHandler: postToServiceHandler,
            postToServiceInstance: postToServiceInstance,
            deleteModel: deleteModel,
            deleteItem: deleteItem,
            getNewID: getNewID,
            errorToString: errorToString,
            exportModel: exportModel,
            importModel: importModel
        };

        return service;

        function getModels() { return getStuff(API.models, 'models'); }
        function getItems(modelID) { return getStuff(API.items(modelID), 'items'); }
        function getModelDiff(modelID, version) { return getStuff(API.modelDiff(modelID,version), 'model diff'); }
        function getNewID() { return getStuff(API.newID, 'get new id'); }
        function revertModel(modelID, version) { return getStuff(API.revertModel(modelID, version), 'revert model'); }
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
                var msg = 'REST Service: Query for ' + itemKind  + ' failed. ' + JSON.stringify(error.data);
                logger.error(msg);
                return $q.reject(msg);
            }
        }

        function postToModelHandler(data) { return postStuff(API.models, 'model handler', data); }
        function postToModel(modelID, data) { return postStuff(API.model(modelID), 'model actor', data); }
        function postItem(item, modelID) { return postStuff(API.item(modelID, item.id), 'model actor', item); }
        function postItems(data, modelID) { return postStuff(API.items(modelID), 'model actor', data); }
        function postToServiceHandler(data) { return postStuff(API.serviceHandler, 'service handler', data); }
        function postToServiceInstance(data, serviceID) { return postStuff(API.serviceInstance(serviceID), serviceID, data); }

        function postStuff(restURL, receiver, data) {
            return $http.post(restURL, data)
                .then(success)
                .catch(fail);

            function success(response) {
                logger.info('REST Service: Successfully posted data to ' + receiver + '.');
                return response.data;
            }

            function fail(error) {
                console.log("got an error");
                console.log(error.data);
                var msg = 'An error occurred in ' + receiver  + ':' + errorToString(error.data);
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

        function exportModel(modelID){return getStuff(API.modelExport(modelID), 'export model')}
        function importModel(modelData){return postStuff(API.modelImport, 'import model', modelData)}



        /**
         * Move to some place good
         * @param error the SPError
         */
        function errorToString(error){
            var msg = "";
            if (!angular.isUndefined(error.errors)){
                angular.forEach(error.errors, function(e){
                    msg = msg + '<br/>\n' + e.error
                })
            }
            if (!angular.isUndefined(error.error)){
                msg = msg + '\n' + error.error
            }
            if (!angular.isUndefined(error.serviceError)){
                msg = msg + '<br/>\n' + 'Error from Service: '+error.service+ " request id: "+error.reqID;
                msg = msg + '<br/>\n' + errorToString(error.serviceError)
            }
            if (!angular.isUndefined(error.conflicts)){
                msg = msg + '<br/>\n' + 'The following IDs could not be updated: \n';
                angular.forEach(error.conflicts, function(id){
                    msg = msg + '<br/>\n' + id
                })
            }
            return msg;
        }
    }
})();
