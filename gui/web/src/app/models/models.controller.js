(function () {
    'use strict';

    angular
        .module('app.models')
        .controller('ModelsController', ModelsController);

    ModelsController.$inject = ['modelService', '$state', 'logger'];
    /* @ngInject */
    function ModelsController(modelService, $state, logger) {
        var vm = this;
        vm.title = $state.current.title;
        vm.models = modelService.models;
        vm.modelService = modelService;
        vm.displayedModels = [];
        vm.updateName = modelService.updateName;
        vm.setActiveModel = setActiveModel;
        vm.widget = {title: "Model list"};

        activate();

        function activate() {
            logger.info('Models Controller: Activated Models view');
        }

        function setActiveModel(m) {
            modelService.setActiveModel(m);
            $state.go('dashboard');
        }

    }
})();
