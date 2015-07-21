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
            activeModel: activeModel
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
                if(data.event === 'Creation') {
                    console.log(data);
                    models.push(data.modelInfo);
                    logger.info('Added a model with id ' + data.modelInfo.id + ' and name ' + data.modelInfo.name + '.');
                } else if(data.event === 'Deletion') {
                    models.splice(_.findIndex(models, { id: data.id }), 1);
                    logger.info('Removed a model with id ' + data.id + '.');
                }
            }
        }

    }
})();
