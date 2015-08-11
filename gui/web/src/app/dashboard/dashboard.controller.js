(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['logger', '$sessionStorage', '$state'];
    /* @ngInject */
    function DashboardController(logger, $sessionStorage, $state) {
        var vm = this;
        vm.title = $state.current.title;
        vm.addWidget = addWidget;
        vm.closeWidget = closeWidget;
        vm.storage = $sessionStorage.$default({
            widgets: []
        });
        vm.gridsterOptions = {
            outerMargin: false,
            draggable: {
                handle: '.panel-heading'
            }
        };
        vm.widgetKinds = [
            { sizeX: 2, sizeY: 2, title: 'Item List', template: 'app/item-list/item-list.html' },
            { sizeX: 2, sizeY: 2, title: 'SOP Maker' },
            { sizeX: 1, sizeY: 1, title: 'Item Explorer' },
            { sizeX: 1, sizeY: 1, title: 'Service List' },
            { sizeX: 2, sizeY: 1, title: 'Runtime List' }
        ];

        activate();

        function addWidget(widgetKind) {
            vm.storage.widgets.push(widgetKind)
        }

        function closeWidget(widget) {
            vm.storage.widgets.splice(vm.storage.widgets.indexOf(widget), 1);
        }

        function activate() {
            logger.info('Activated dashboard view');
        }

    }
})();
