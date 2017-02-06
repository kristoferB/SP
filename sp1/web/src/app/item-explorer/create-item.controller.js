(function () {
    'use strict';

    angular
        .module('app.itemExplorer')
        .controller('CreateItemController', CreateItemController);

    CreateItemController.$inject = ['$modalInstance', 'itemKind'];
    /* @ngInject */
    function CreateItemController($modalInstance, itemKind) {
        var vm = this;
        vm.name = '';
        vm.save = save;
        vm.cancel = cancel;
        vm.itemKind = itemKind;

        function save() {
            $modalInstance.close(vm.name);
        }

        function cancel() {
            $modalInstance.dismiss('cancel');
        }

    }
})();
