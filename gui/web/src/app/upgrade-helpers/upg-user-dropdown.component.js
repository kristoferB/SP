(function() {
    'use strict';

    angular
        .module('app')
        .component('upgUserDropdown', upgUserDropdownOptions());

    /* @ngInject */
    function upgUserDropdownOptions() {
        var options = {
            scope: {}, // possibly unused since component-refactor
            //controller: UpgUserDropdownController,
            //controllerAs: 'vm',
            restrict: 'E', // must be set to 'E' for upgradability
            templateUrl: 'app/upgrade-helpers/upg-user-dropdown.html'
        };

        return options;
    }

    ///* @ngInject */
    //UpgUserDropdownController.$inject = [];

    //function UpgUserDropdownController() {
    //}
})();
