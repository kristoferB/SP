(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['logger', '$sessionStorage', '$state', '$timeout'];
    /* @ngInject */
    function DashboardController(logger, $sessionStorage, $state, $timeout) {
        var vm = this;
        vm.title = $state.current.title;
        vm.addWidget = addWidget;
        vm.closeWidget = closeWidget;
        vm.gridsterOptions = {
            outerMargin: false,
            swapping: true,
            draggable: {
                enabled: false,
                handle: '.panel-heading'
            }
        };
        vm.storage = $sessionStorage.$default({
            widgets: []
        });
        vm.widgetKinds = [
            {sizeX: 1, sizeY: 2, title: 'Item Explorer', template: 'app/item-explorer/item-explorer.html'},
            {sizeX: 2, sizeY: 2, title: 'Item Editor', template: 'app/item-editor/item-editor.html'},
            {sizeX: 1, sizeY: 1, title: 'Service List'},
            {sizeX: 2, sizeY: 1, title: 'Runtime List'},
            {sizeX: 2, sizeY: 2, title: 'SOP Maker'}
        ];

        activate();

        function activate() {
            enableWidgetDrag();
            logger.info('Activated dashboard view');
        }

        function enableWidgetDrag() {
            $timeout(function() {
                vm.gridsterOptions.draggable.enabled = true;
            }, 500, false);
        }

        function addWidget(widgetKind) {
            vm.storage.widgets.push(widgetKind);
        }

        function closeWidget(widget) {
            vm.storage.widgets.splice(vm.storage.widgets.indexOf(widget), 1);
        }

    }
})();
