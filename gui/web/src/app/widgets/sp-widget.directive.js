(function () {
    'use strict';

    angular
        .module('app.widgets')
        .directive('spWidget', spWidget);

    spWidget.$inject = [];
    /* @ngInject */
    function spWidget () {
        var directive = {
            restrict: 'E',
            templateUrl: 'app/widgets/sp-widget.html',
            controller: SPWidgetController,
            controllerAs: 'vm',
            scope: {},
            bindToController: {
                title: '=',
                bodyTemplate: '@'
            }
        };
        return directive;
    }

    function SPWidgetController($scope) {
        var vm = this;
        vm.$parent = $scope.$parent;
    }

})();
