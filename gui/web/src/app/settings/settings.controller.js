(function () {
    'use strict';

    angular
        .module('app.settings')
        .controller('SettingsController', SettingsController);

    SettingsController.$inject = ['logger'];
    /* @ngInject */
    function SettingsController(logger) {
        var vm = this;
        vm.title = 'Settings';

        activate();

        function activate() {
            logger.info('Activated Settings View');
        }
    }
})();
