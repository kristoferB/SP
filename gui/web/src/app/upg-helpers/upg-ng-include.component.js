(function() {
    'use strict';

    angular
        .module('app')
        .component('upgNgInclude', upgNgIncludeOptions());

    /* @ngInject */
    function upgNgIncludeOptions() {
        var options = {
            //scope: {}, // possibly unused since component-refactor
            bindings: { // must use bindings, not bindToController
                src: '='
            },
            restrict: 'E', // must be set to 'E' for upgradability
            template: '<ng-include src="$ctrl.src" include-replace></ng-include>'
        };

        return options;
    }
})();
