(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['logger', '$state', '$timeout', 'dashboardService', '$rootScope'];
    /* @ngInject */
    function DashboardController(logger, $state, $timeout, dashboardService, $rootScope) {
        var vm = this;
        vm.dashboard = dashboardService.getDashboard(1);
        vm.title = $state.current.title;
        vm.gridsterOptions = {
            outerMargin: false,
            columns: 12,
            swapping: true,
            draggable: {
                enabled: false,
                handle: '.panel-heading'
            }
        };
        vm.dashboardService = dashboardService;

        activate();

        function activate() {
            enableWidgetDrag();
                closeSOPMakerWidgetOnItemEvent('itemDeletion');
            closeSOPMakerWidgetsOnModelChange();
            logger.info('Dashboard Controller: Activated Dashboard view.');
        }

        function enableWidgetDrag() {
            $timeout(function() {
                vm.gridsterOptions.draggable.enabled = true;
            }, 500, false);
        }

        function closeSOPMakerWidgetOnItemEvent(itemEvent) {
            $rootScope.$on(itemEvent, function(event, item) {
                if(item.isa == "SOPSpec") {
                    for(var i = 0; i < vm.dashboard.widgets.length; i++) {
                        var widget = vm.dashboard.widgets[i];
                        if(!_.isUndefined(widget.storage)) {
                            if(!_.isUndefined(widget.storage.sopSpecID)) {
                                if(item.id == widget.storage.sopSpecID) {
                                    vm.dashboardService.closeWidget(widget.id);
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }

        function closeSOPMakerWidgetsOnModelChange() {
            $rootScope.$on('modelChanged', function(event, model) {
                for(var i = 0; i < vm.dashboard.widgets.length; i++) {
                    var widget = vm.dashboard.widgets[i];
                    if(widget.title == "SOP Maker") {
                        vm.dashboardService.closeWidget(widget.id);
                    }
                }
            });
        }
    }
})();
