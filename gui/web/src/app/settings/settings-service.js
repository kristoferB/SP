(function () {
    'use strict';

    angular
        .module('app.settings')
        .factory('settingsService', settingsService);

    settingsService.$inject = ['$sessionStorage', '$http', 'dashboardService'];
    /* @ngInject */
    function settingsService($sessionStorage, $http, dashboardService) {
        var service = {
            storage: $sessionStorage.$default({
                currentColor: "default_white",
                availableColors: ["default_white", "blue", "dark", "happy"]
            }),

            getColorOptions: getColorOptions,
            updateColorTheme: updateColorTheme
        };

        activate();

        return service;

        function activate() {
            updateColorTheme();
        }

        function updateColorTheme(){
            //downGradedThemeService.setColorTheme(service.storage.currentColor);
        }

        function getColorOptions(){
            return $sessionStorage.availableColors;
        }
    }
})();

