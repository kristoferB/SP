(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('CreateDashboardController', CreateDashboardController);

    CreateDashboardController.$inject = ['$uibModalInstance'];
    /* @ngInject */
    function CreateDashboardController($uibModalInstance) {
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
