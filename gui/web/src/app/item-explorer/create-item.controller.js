(function () {
    'use strict';

    angular
        .module('app.itemExplorer')
        .controller('CreateItemController', CreateItemController);

    CreateItemController.$inject = ['$uibModalInstance', 'itemKind'];
    /* @ngInject */
    function CreateItemController($uibModalInstance, itemKind) {
        var vm = this;
        vm.name = '';
        vm.save = save;
        vm.cancel = cancel;
        vm.itemKind = itemKind;

        function save() {
            $uibModalInstance.close(vm.name);
        }

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }

    }
})();
