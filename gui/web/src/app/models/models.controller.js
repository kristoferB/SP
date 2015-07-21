(function () {
    'use strict';

    angular
        .module('app.models')
        .controller('ModelsController', ModelsController);

    ModelsController.$inject = ['model'];
    /* @ngInject */
    function ModelsController(model) {
        var vm = this;
        vm.title = 'Models';
        vm.models = model.models;
        vm.createModel = model.createModel;
        vm.deleteModel = model.deleteModel;
        vm.updateName = model.updateName;

    }
})();
