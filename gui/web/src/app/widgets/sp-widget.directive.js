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
            link: link,
            transclude: true,
            replace: true,
            bindToController: {
                widgetTitle: '=',
                closeHandler: '&'
            }
        };
        return directive;

        function link(scope, element) {
            scope.vm.showCloseBtn = element.attr('close-handler');
        }
    }

    SPWidgetController.$inject = [];

    function SPWidgetController() {
        var vm = this;
    }

})();
