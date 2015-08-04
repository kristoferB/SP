(function () {
    'use strict';

    angular
        .module('app.models')
        .controller('ModelsController', ModelsController);

    ModelsController.$inject = ['model', '$window', '$modal', '$state', 'logger'];
    /* @ngInject */
    function ModelsController(model, $window, $modal, $state, logger) {
        var vm = this;
        vm.title = 'Models';
        vm.models = model.models;
        vm.model = model;
        vm.displayedModels = [];
        vm.createModel = createModel;
        vm.updateName = model.updateName;
        vm.deleteModel = deleteModel;
        vm.setActiveModel = setActiveModel;

        activate();

        function activate() {
            logger.info('Activated models view');
        }

        function setActiveModel(m) {
            model.setActiveModel(m);
            $state.go('dashboard');
        }

        function deleteModel(id) {
            var sure = $window.confirm('Are you sure you want to delete the whole model?');
            if (sure) {
                model.deleteModel(id);
            }
        }

        function createModel() {
            var modalInstance = $modal.open({
                templateUrl: '/app/models/createmodel.html',
                controller: 'CreateModelController',
                controllerAs: 'vm'
            });

            modalInstance.result.then(function(chosenName) {
                model.createModel(chosenName);
            });
        }

    }
})();
