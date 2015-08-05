(function () {
    'use strict';

    angular
        .module('app.models')
        .controller('ModelsController', ModelsController);

    ModelsController.$inject = ['modelService', '$window', '$modal', '$state', 'logger'];
    /* @ngInject */
    function ModelsController(modelService, $window, $modal, $state, logger) {
        var vm = this;
        vm.title = 'Models';
        vm.models = modelService.models;
        vm.modelService = modelService;
        vm.displayedModels = [];
        vm.createModel = createModel;
        vm.updateName = modelService.updateName;
        vm.deleteModel = deleteModel;
        vm.setActiveModel = setActiveModel;

        activate();

        function activate() {
            logger.info('Activated models view');
        }

        function setActiveModel(m) {
            modelService.setActiveModel(m);
            $state.go('dashboard');
        }

        function deleteModel(id) {
            var sure = $window.confirm('Are you sure you want to delete the whole model?');
            if (sure) {
                modelService.deleteModel(id);
            }
        }

        function createModel() {
            var modalInstance = $modal.open({
                templateUrl: '/app/models/createmodel.html',
                controller: 'CreateModelController',
                controllerAs: 'vm'
            });

            modalInstance.result.then(function(chosenName) {
                modelService.createModel(chosenName);
            });
        }

    }
})();
