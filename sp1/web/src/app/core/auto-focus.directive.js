(function() {
    'use strict';

    angular
        .module('app.core')
        .directive('autoFocus', autoFocus);

    /* @ngInject */
    function autoFocus($timeout) {
        var directive = {
            restrict: 'A',
            link: linkFunc
        };
        return directive;

        function linkFunc(scope, element) {
            $timeout(function () {
                element[0].focus();
            }, 10);
        }
    }
})();
