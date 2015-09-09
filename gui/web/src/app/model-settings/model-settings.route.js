(function() {
    'use strict';

    angular
        .module('app.modelSettings')
        .run(appRun);

    appRun.$inject = ['routerHelper'];
    /* @ngInject */
    function appRun(routerHelper) {
        routerHelper.configureStates(getStates());
    }

    function getStates() {
        return [
            {
                state: 'model-settings',
                config: {
                    url: '/model-settings',
                    templateUrl: 'app/model-settings/model-settings.html',
                    controller: 'ModelSettingsController',
                    controllerAs: 'vm',
                    title: 'ModelSettings',
                    settings: {
                    }
                }
            }
        ];
    }
})();
