(function() {
    'use strict';

    angular
        .module('app.core')
        .directive('autoFocus', autoFocus);

    /* @ngInject */
    function autoFocus($timeout) {
        var directive = {
            link: function (scope, element) {
                $timeout(function () {
                    element[0].focus();
                }, 10);
            }
        };
        return directive;
    }
})();
