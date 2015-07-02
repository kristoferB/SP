(function () {
    'use strict';

    angular
        .module('app.models')
        .controller('CreateModelController', CreateModelController);

    CreateModelController.$inject = ['$modalInstance'];
    /* @ngInject */
    function CreateModelController($modalInstance) {
        var vm = this;
        vm.name = '';
        vm.save = save;
        vm.cancel = cancel;

        function save() {
            $modalInstance.close(vm.name);
        }

        function cancel() {
            $modalInstance.dismiss('cancel');
        }

    }
})();
