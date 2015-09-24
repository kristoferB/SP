/**
 * Created by patrik on 2015-09-22.
 */
(function () {
    'use strict';

    angular
        .module('app.spServices')
        .directive('spServicesForm', spServicesForm);

    spServicesForm.$inject = ['$compile','logger'];
    /* @ngInject */
    function spServicesForm($compile, logger) {

        var directive = {
            link: function(scope, element, attrs) {
                var template = attrs.form;
                var linkFn = $compile(template);
                var content = linkFn(scope);
                element.append(content);
            }
        };

        return directive;

    }

})();
