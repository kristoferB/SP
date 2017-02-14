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
        vm.importTitle = {title: "Import / Export"};
        vm.options = {mode: 'code'};
        vm.modelJson = {};
        vm.editorLoaded = editorLoaded;
        vm.editor = null;
        vm.exportModel = exportModel;
        vm.importModel = importModel;


        activate();

        function activate() {
            logger.info('Models Controller: Activated Models view');
        }

        function setActiveModel(m) {
            modelService.setActiveModel(m);
            $state.go('dashboard');
        }

        function editorLoaded(editorInstance) {
            vm.editor = editorInstance;
            editorInstance.setName('Selected items');
        }

        function exportModel(id){
            modelService.exportModel(id).then(function(data){
                vm.modelJson = data;
            });
        }

        function importModel(){
            modelService.importModel(vm.modelJson);
        }
    }
})();
