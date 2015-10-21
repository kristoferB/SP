(function () {
    'use strict';

    angular
        .module('app.settings')
        .controller('SettingsController', SettingsController);

    SettingsController.$inject = ['logger', '$state', 'settingsService'];
    /* @ngInject */
    function SettingsController(logger, $state, settingsService) {
        var vm = this;
        vm.title = $state.current.title;
        vm.widget = {title: "Appearance"};
        vm.themes = settingsService.themes;
        vm.theme = settingsService.storage.theme;

        activate();

        function activate() {
            logger.info('Activated Settings View');
        }
    }
})();
