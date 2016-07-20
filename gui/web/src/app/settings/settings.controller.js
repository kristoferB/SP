(function () {
    'use strict';

    angular
        .module('app.settings')
        .controller('SettingsController', SettingsController);

    SettingsController.$inject = ['logger', '$state', 'settingsService', '$localStorage'];
    /* @ngInject */
    function SettingsController(logger, $state, settingsService, $localStorage) {
        var vm = this;
        vm.title = $state.current.title;
        vm.widget = {title: "Appearance"};
        vm.settingsService = settingsService;
        vm.colorOptions = settingsService.getColorOptions();
        vm.updateColorTheme = settingsService.updateColorTheme;
        activate();

        function activate() {
            logger.info('Activated Settings View');
        }
    }
})();
