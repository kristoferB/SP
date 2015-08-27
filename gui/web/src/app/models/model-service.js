(function () {
    'use strict';

    angular
        .module('app.models')
        .factory('modelService', modelService);

    modelService.$inject = ['$q', 'logger', 'restService', 'eventService', '$sessionStorage', '$rootScope'];
    /* @ngInject */
    function modelService($q, logger, restService, eventService, $sessionStorage, $rootScope) {
        var service = {
            activeModel: null,
            models: [],
            idOfLatestModel: '',
            setActiveModel: setActiveModel,
            updateName: updateName,
            createModel: createModel,
            deleteModel: deleteModel,
            getModel: getModel
        };
        var storage = $sessionStorage.$default({
            activeModelID: null
        });

        activate();

        return service;

        function activate() {
            listenToModelEvents();
            var promises = [getAllModels()];
            return $q.all(promises).then(function() {
                logger.info('Model Service: Loaded ' + service.models.length + ' models through REST.');
                restoreActiveModel(storage.activeModelID);
            });
        }

        function getAllModels() {
            return restService.getModels().then(function (data) {
                service.models.length = 0;
                service.models.push.apply(service.models, data);
            });
        }

        function setActiveModel(model) {
            service.activeModel = model;
            storage.activeModelID = model.id;
            logger.info('Model Service: Active model was set to ' + model.name + '.');
            $rootScope.$broadcast('modelChanged', service.activeModel);
        }

        function restoreActiveModel() {
            if (storage.activeModelID !== null) {
                var model = getModel(storage.activeModelID);
                if (model === null) {
                    logger.error('Model Service: The previously active model is no longer available.')
                } else {
                    setActiveModel(model);
                }
            }
        }

        function listenToModelEvents() {
            eventService.addListener('modelService', onModelEvent);

            function onModelEvent(data) {
                if (data.event === 'update') {
                    var model = getModel(data.modelInfo.id);
                    var oldName = model.name;
                    angular.extend(model, data.modelInfo);
                    logger.info('Model Service: Updated name and/or attributes for model ' + oldName + '.');
                } else if (data.event === 'creation') {
                    service.models.push(data.modelInfo);
                    service.idOfLatestModel = data.modelInfo.id;
                    logger.info('Model Service: Added a model with name ' + data.modelInfo.name + '.');
                } else if (data.event === 'deletion') {
                    var name = getModel(data.id).name;
                    service.models.splice(_.findIndex(service.models, {id: data.id}), 1);
                    logger.info('Model Service: Removed model ' + name + '.');
                }
            }
        }

        function getModel(id) {
            var index = _.findIndex(service.models, {id: id});
            if (index === -1) {
                return null
            } else {
                return service.models[index];
            }
        }

        function createModel(name) {
            var newModel = {
                name: name
            };
            restService.postToModelHandler(newModel);
        }

        function deleteModel(id) {
            restService.deleteModel(id);
        }

        function updateName(model, name) {
            var data = {};
            angular.extend(data, model);
            data.name = name;
            return restService.postToModel(model.id, data)
                .then(success)
                .catch(fail);

            function success() {
                return true;
            }

            function fail(error) {
                return error;
            }
        }

    }
})();
