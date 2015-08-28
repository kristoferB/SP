(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['logger', '$sessionStorage', '$state', '$timeout', '$scope'];
    /* @ngInject */
    function DashboardController(logger, $sessionStorage, $state, $timeout, $scope) {
        var vm = this;
        vm.title = $state.current.title;
        vm.addWidget = addWidget;
        vm.gridsterOptions = {
            outerMargin: false,
            swapping: true,
            draggable: {
                enabled: false,
                handle: '.panel-heading'
            }
        };
        vm.storage = $sessionStorage.$default({
            widgets: [],
            widgetID: 1
        });
        vm.widgetKinds = [
            {sizeX: 2, sizeY: 2, title: 'Item Explorer', template: 'app/item-explorer/item-explorer.html'},
            {sizeX: 2, sizeY: 2, title: 'Item Editor', template: 'app/item-editor/item-editor.html'},
            {sizeX: 1, sizeY: 1, title: 'Service List'},
            {sizeX: 2, sizeY: 1, title: 'Runtime List'},
            {sizeX: 2, sizeY: 2, title: 'SOP Maker'}
        ];

        activate();

        function activate() {
            enableWidgetDrag();
            logger.info('Dashboard Controller: Activated Dashboard view.');
            $scope.$on('closeWidget', function(widgetID) {
                var arrayIndex = _.findIndex(vm.storage.widgets, {id: widgetID});
                vm.storage.widgets.splice(arrayIndex, 1);
            });
        }

        function enableWidgetDrag() {
            $timeout(function() {
                vm.gridsterOptions.draggable.enabled = true;
            }, 500, false);
        }

        function addWidget(widgetKind) {
            var widget = angular.copy(widgetKind, {});
            widget.id = vm.storage.widgetID;
            vm.storage.widgetID++;
            vm.storage.widgets.push(widget);
            logger.info('Dashboard Controller: Added an ' + widget.title + ' widget with index '
                + vm.storage.widgetID + ' to Dashboard.');
        }
    }
})();
