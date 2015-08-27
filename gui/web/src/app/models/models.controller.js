(function () {
    'use strict';

    angular
        .module('app.models')
        .controller('ModelsController', ModelsController);

    ModelsController.$inject = ['modelService', '$modal', '$state', 'logger'];
    /* @ngInject */
    function ModelsController(modelService, $modal, $state, logger) {
        var vm = this;
        vm.title = $state.current.title;
        vm.models = modelService.models;
        vm.modelService = modelService;
        vm.displayedModels = [];
        vm.createModel = createModel;
        vm.updateName = modelService.updateName;
        vm.setActiveModel = setActiveModel;

        activate();

        function activate() {
            logger.info('Models Controller: Activated Models view');
        }

        function setActiveModel(m) {
            modelService.setActiveModel(m);
            $state.go('dashboard');
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
