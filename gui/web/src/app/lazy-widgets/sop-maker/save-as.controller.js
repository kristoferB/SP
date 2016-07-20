(function () {
    'use strict';

    angular
        .module('app.sopMaker')
        .controller('SaveAsController', SaveAsController);

    SaveAsController.$inject = ['$modalInstance', 'item'];
    /* @ngInject */
    function SaveAsController($modalInstance, item) {
        var vm = this;
        vm.name = '';
        vm.save = save;
        vm.cancel = cancel;

        function save() {
            item.name = vm.name;
            $modalInstance.close(item);
        }

        function cancel() {
            $modalInstance.dismiss('cancel');
        }

    }
})();
