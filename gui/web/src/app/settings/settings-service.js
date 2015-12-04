(function () {
    'use strict';

    angular
        .module('app.settings')
        .factory('settingsService', settingsService);

    settingsService.$inject = ['$localStorage'];
    /* @ngInject */
    function settingsService($localStorage) {
        var service = {
            storage: $localStorage.$default({
                theme: 'default'
            }),
            themes: ['default', 'blue', 'dark']
        };

        activate();

        return service;

        function activate() {

        }

    }
})();
