(function () {
    'use strict';

    angular
        .module('app.settings')
        .controller('SettingsController', SettingsController);

    SettingsController.$inject = ['logger', '$state'];
    /* @ngInject */
    function SettingsController(logger, $state) {
        var vm = this;
        vm.title = $state.current.title;

        activate();

        function activate() {
            logger.info('Activated Settings View');
        }
    }
})();
