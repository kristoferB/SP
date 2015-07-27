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
            transclude: true,
            replace: true,
            bindToController: {
                widgetTitle: '='
            }
        };
        return directive;
    }

    SPWidgetController.$inject = [];

    function SPWidgetController() {
        var vm = this;
    }

})();
