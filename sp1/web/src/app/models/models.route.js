(function() {
    'use strict';

    angular
        .module('app.models')
        .run(appRun);

    appRun.$inject = ['routerHelper'];
    /* @ngInject */
    function appRun(routerHelper) {
        routerHelper.configureStates(getStates());
    }

    function getStates() {
        return [
            {
                state: 'models',
                config: {
                    url: '/models',
                    templateUrl: 'app/models/models.html',
                    controller: 'ModelsController',
                    controllerAs: 'vm',
                    title: 'Models',
                    settings: {}
                }
            }
        ];
    }
})();
