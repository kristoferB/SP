/**
 * Created by daniel on 2015-08-14.
 */
(function() {
    'use strict';

    angular
        .module('app.core')
        .directive('includeReplace', includeReplace);

    /* @ngInject */
    function includeReplace() {
        var directive = {
            require: 'ngInclude',
            restrict: 'A',
            link: link
        };
        return directive;

        function link(scope, el) {
            el.replaceWith(el.children());
        }
    }
})();
