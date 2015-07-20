(function () {
    'use strict';

    angular
        .module('app.models')
        .controller('ModelsController', ModelsController);

    ModelsController.$inject = ['rest', 'model'];
    /* @ngInject */
    function ModelsController(rest, model) {
        var vm = this;
        vm.title = 'Models';
        vm.models = model.models;
        vm.createModel = createModel;
        vm.deleteModel = deleteModel;

        activate();

        function activate() {}

        function createModel() {
            const newModel = {
              name: ''
            };
            rest.postToModelHandler(newModel);
        }

        function deleteModel(model) {
            rest.deleteModel(model.id);
        }
    }
})();
