(function () {
    'use strict';

    angular
        .module('app.models')
        .factory('modelService', modelService);

    modelService.$inject = ['$q', 'logger', 'restService', 'eventService'];
    /* @ngInject */
    function modelService($q, logger, restService, eventService) {
        var models = [];
        var activeModel = null;
        var service = {
            models: models,
            idOfLatestModel: '',
            getActiveModel: getActiveModel,
            setActiveModel: setActiveModel,
            updateName: updateName,
            createModel: createModel,
            deleteModel: deleteModel,
            getModel: getModel
        };

        activate();

        return service;

        function activate() {
            listenToModelHandlerEvents();
            var promises = [getAllModels()];
            return $q.all(promises).then(function() {
                logger.info('Loaded ' + models.length + ' models through REST.');
            });
        }

        function getAllModels() {
            return restService.getModels().then(function (data) {
                models.length = 0;
                models.push.apply(models, data);
            });
        }

        function getActiveModel() {
            return activeModel;
        }

        function setActiveModel(model) {
            activeModel = model;
            logger.info('Set active model to ' + model.name + '.');
        }

        function listenToModelHandlerEvents() {
            eventService.addListener('ModelHandler', onModelHandlerEvent);

            function onModelHandlerEvent(data) {
                if (data.event === 'Update') {
                    var model = getModel(data.modelInfo.id);
                    var oldName = model.name;
                    angular.extend(model, data.modelInfo);
                    logger.info('Updated name and/or attributes for model ' + oldName + '.');
                } else if (data.event === 'Creation') {
                    models.push(data.modelInfo);
                    service.idOfLatestModel = data.modelInfo.id;
                    logger.info('Added a model with name ' + data.modelInfo.name + '.');
                } else if (data.event === 'Deletion') {
                    var name = getModel(data.id).name;
                    models.splice(getModelArrayIndex(data.id), 1);
                    logger.info('Removed model ' + name + '.');
                }
            }
        }

        function getModelArrayIndex(id) {
            return _.findIndex(models, {id: id});
        }

        function getModel(id) {
            return models[getModelArrayIndex(id)];
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
