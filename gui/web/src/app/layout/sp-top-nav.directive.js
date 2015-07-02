(function() {
    'use strict';

    angular
        .module('app.layout')
        .directive('htTopNav', htTopNav);

    /* @ngInject */
    function htTopNav () {
        var directive = {
            scope: {},
            bindToController: {
                'navline': '='
            },
            controller: TopNavController,
            controllerAs: 'vm',
            restrict: 'EA',
            templateUrl: 'app/layout/sp-top-nav.html'
        };

        return directive;
    }

    /* @ngInject */
    TopNavController.$inject = ['model'];

    function TopNavController(model) {
        var vm = this;
        vm.model = model;
    }

})();
