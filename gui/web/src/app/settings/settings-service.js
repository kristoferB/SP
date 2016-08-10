(function () {
    'use strict';

    angular
        .module('app.settings')
        .factory('settingsService', settingsService);

    settingsService.$inject = ['$localStorage', '$http', 'dashboardService', 'themeService'];
    /* @ngInject */
    function settingsService($localStorage, $http, dashboardService, themeService) {

        var service = {
            storage: $localStorage.$default({
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
            themeService.setColorTheme(service.storage.currentColor);
        }

        function getColorOptions(){
            return $localStorage.availableColors;
        }
    }
})();

