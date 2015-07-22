(function () {
    'use strict';

    angular
        .module('app.models')
        .factory('model', model);

    model.$inject = ['$q', 'logger', 'rest', 'event'];
    /* @ngInject */
    function model($q, logger, rest, event) {
        var models = [];
        var activeModel = null;
        var service = {
            models: models,
            activeModel: activeModel,
            updateName: updateName,
            createModel: createModel,
            deleteModel: deleteModel
        };

        activate();

        return service;

        function activate() {
            listenToModelHandlerEvents();
            var promises = [getAllModels()];
            return $q.all(promises).then(function() {
                logger.info('Activated Models View');
            });
        }

        function getAllModels() {
            return rest.getModels().then(function (data) {
                models.length = 0;
                models.push.apply(models, data);
            });
        }

        function listenToModelHandlerEvents() {
            event.addListener('ModelHandler', onModelHandlerEvent);

            function onModelHandlerEvent(data) {
                if(data.event === 'Update') {
                    const model = getModel(data.modelInfo.id);
                    const oldName = model.name;
                    angular.extend(model, data.modelInfo);
                    logger.info('Updated name and/or attributes for model ' + oldName + '.');
                } else if(data.event === 'Creation') {
                    models.push(data.modelInfo);
                    logger.info('Added a model with name ' + data.modelInfo.name + '.');
                } else if(data.event === 'Deletion') {
                    const name = getModel(data.id).name;
                    models.splice(getModelArrayIndex(data.id), 1);
                    logger.info('Removed model ' + name + '.');
                }
            }
        }

        function getModelArrayIndex(id) {
            return _.findIndex(models, { id: id });
        }

        function getModel(id) {
            return models[getModelArrayIndex(id)];
        }

        function createModel(name) {
            const newModel = {
                name: name
            };
            rest.postToModelHandler(newModel);
        }

        function deleteModel(id) {
            rest.deleteModel(id);
        }

        function updateName(model, name) {
            var data = {};
            angular.extend(data, model);
            data.name = name;
            return rest.postToModel(model.id, data)
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
