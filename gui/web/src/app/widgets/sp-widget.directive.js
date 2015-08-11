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
                widgetTitle: '=',
                closeHandler: '&'
            }
        };
        return directive;
    }

    SPWidgetController.$inject = [];

    function SPWidgetController() {
        var vm = this;
        vm.closeButtonVisible = false;
        vm.showCloseButton = showCloseButton;
        vm.hideCloseButton = hideCloseButton;

        function showCloseButton() {
            vm.closeButtonVisible = true;
        }

        function hideCloseButton() {
            vm.closeButtonVisible = false;
        }
    }

})();
