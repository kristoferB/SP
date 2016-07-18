(function() {
    'use strict';

    angular
        .module('app')
        .component('upgUiView', upgUiViewOptions());

    /* @ngInject */
    function upgUiViewOptions() {
        var options = {
            scope: {}, // possibly unused since component-refactor
            controller: UpgUiViewController,
            controllerAs: 'vm',
            restrict: 'E', // must be set to 'E' for upgradability
            templateUrl: 'app/upgrade-helpers/upg-ui-view.html'
        };

        return options;
    }

    /* @ngInject */
    UpgUiViewController.$inject = [];

    function UpgUiViewController() {
    }
})();
