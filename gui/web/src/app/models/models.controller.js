(function () {
    'use strict';

    angular
        .module('app.models')
        .controller('ModelsController', ModelsController);

    ModelsController.$inject = ['$q', 'spTalker', 'logger'];
    /* @ngInject */
    function ModelsController($q, spTalker, logger) {
        var vm = this;
        vm.title = 'Models';
        vm.models = [];
        vm.createModel = createModel;

        activate();

        function activate() {
            var promises = [getModels()];
            return $q.all(promises).then(function() {
                logger.info('Activated Models View');
            });
        }

        function getModels() {
            return spTalker.getModels().then(function (data) {
                vm.models = data;
                return vm.models;
            });
        }

        function deleteModel() {

        }

        function createModel() {
            const newModel = {
              name: ''
            };
            spTalker.postToModelHandler(newModel)
                .then(addModelToTable);

            function addModelToTable(model) {
                vm.models.push(model);
            }
        }
    }
})();
