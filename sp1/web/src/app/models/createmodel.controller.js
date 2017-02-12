(function () {
    'use strict';

    angular
        .module('app.models')
        .controller('CreateModelController', CreateModelController);

    CreateModelController.$inject = ['$uibModalInstance'];
    /* @ngInject */
    function CreateModelController($uibModalInstance) {
        var vm = this;
        vm.name = '';
        vm.save = save;
        vm.cancel = cancel;

        function save() {
            $uibModalInstance.close(vm.name);
        }

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }

    }
})();
