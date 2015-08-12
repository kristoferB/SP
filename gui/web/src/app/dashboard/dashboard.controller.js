(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['logger', '$sessionStorage', '$state', '$rootScope', '$timeout'];
    /* @ngInject */
    function DashboardController(logger, $sessionStorage, $state, $rootScope, $timeout) {
        var vm = this;
        vm.title = $state.current.title;
        vm.addWidget = addWidget;
        vm.closeWidget = closeWidget;
        vm.gridsterOptions = {
            outerMargin: false,
            draggable: {
                enabled: false,
                handle: '.panel-heading'
            }
        };
        vm.storage = $sessionStorage.$default({
            widgets: []
        });
        vm.widgetKinds = [
            { sizeX: 2, sizeY: 2, title: 'Item List', template: 'app/item-list/item-list.html' },
            { sizeX: 2, sizeY: 2, title: 'SOP Maker' },
            { sizeX: 1, sizeY: 1, title: 'Item Explorer' },
            { sizeX: 1, sizeY: 1, title: 'Service List' },
            { sizeX: 2, sizeY: 1, title: 'Runtime List' }
        ];

        activate();

        function activate() {
            enableWidgetDrag();
            logger.info('Activated dashboard view');
        }

        function enableWidgetDrag() {
            $timeout(function() {
                vm.gridsterOptions.draggable.enabled = true;
            }, 100, false);
        }

        function addWidget(widgetKind) {
            vm.storage.widgets.push(widgetKind);
        }

        function closeWidget(widget) {
            vm.storage.widgets.splice(vm.storage.widgets.indexOf(widget), 1);
        }

    }
})();
