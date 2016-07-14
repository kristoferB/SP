(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .factory('dashboardService', dashboardService);

    dashboardService.$inject = ['$sessionStorage', 'logger'];
    /* @ngInject */
    function dashboardService($sessionStorage, logger) {
        var service = {
            addDashboard: addDashboard,
            getDashboard: getDashboard,
            removeDashboard: removeDashboard,
            addWidget: addWidget,
            getWidget: getWidget,
            closeWidget: closeWidget,
            storage: $sessionStorage.$default({
                dashboards: [{
                    id: 1,
                    name: 'My Board',
                    widgets: []
                }],
                widgetID: 1,
                dashboardID: 2
            }),

            setPanelLock: setPanelLock,
            setPanelMargins: setPanelMargins,
            gridsterOptions : {
                outerMargin: false,
                columns: 12,
                swapping: true,
                margins: [0,0],
                floating: false,
                pushing: false,
                draggable: {
                    enabled: false,
                    handle: '.panel-heading'
                },
                resizable:{
                    enabled: false
                }
            },

            widgetKinds: [
                {sizeX: 4, sizeY: 4, title: 'Item Explorer', template: 'app/item-explorer/item-explorer.html'},
                {sizeX: 4, sizeY: 4, title: 'Item Editor', template: 'app/item-editor/item-editor.html'},
                {sizeX: 6, sizeY: 4, title: 'Condition Editor', template: 'app/condition-editor/condition-editor.html'},
                {sizeX: 4, sizeY: 4, title: 'SOP Maker', template: 'app/sop-maker/sop-maker.html'},
                {sizeX: 6, sizeY: 4, title: 'Service List', template: 'app/spServices/spServices.html'},
                {sizeX: 6, sizeY: 4, title: 'Trajectories', template: 'app/trajectories/trajectories.html'},
                {sizeX: 6, sizeY: 4, title: 'OPC Runner', template: 'app/opc-runner/opc-runner.html'},
                {sizeX: 4, sizeY: 4, title: 'Process Simulate', template: 'app/process-simulate/process-simulate.html'},
                {sizeX: 4, sizeY: 4, title: 'Operation Control', template: 'app/operation-control/operation-control.html'},
                {sizeX: 4, sizeY: 4, title: 'kubInputGUI', template: 'app/kubInputGUI/kubInputGUI.html'},
                {sizeX: 4, sizeY: 4, title: 'operatorInstGUI', template: 'app/operatorInstGUI/operatorInstGUI.html'},
                {sizeX: 4, sizeY: 4, title: 'ResetGUI', template: 'app/Tobbe2/Tobbe2.html'},
                {sizeX: 4, sizeY: 4, title: 'Active Order', template: 'app/active-order/active-order.html'},
                {sizeX: 4, sizeY: 4, title: 'Robot Cycle Analysis', template: 'app/robot-cycle-analysis/robot-cycle-analysis.html'}
    
           ]
        };

        activate();

        return service;

        function activate() {

        }

        function addDashboard(name) {
            var dashboard = {
                id: service.storage.dashboardID++,
                name: name,
                widgets: []
            };
            logger.info('Dashboard Controller: Added a dashboard with name ' + dashboard.title + ' and index '
                + dashboard.id + '.');
        }

        function getDashboard(id) {
            var index = _.findIndex(service.storage.dashboards, {id: id});
            if (index === -1) {
                return null
            } else {
                return service.storage.dashboards[index];
            }
        }

        function removeDashboard(id) {
            var index = _.findIndex(service.storage.dashboards, {id: id});
            service.storage.dashboards.splice(index, 1);
        }

        function addWidget(dashboard, widgetKind, additionalData) {
            var widget = angular.copy(widgetKind, {});
            widget.id = service.storage.widgetID++;
            if (additionalData !== undefined) {
                widget.storage = additionalData;
            }
            dashboard.widgets.push(widget);
            logger.log('Dashboard Controller: Added a ' + widget.title + ' widget with index '
                + widget.id + ' to dashboard ' + dashboard.name + '.');
        }

        function getWidget(id) {
            var widget = null;
            for(var i = 0; i < service.storage.dashboards.length; i++) {
                var dashboard = service.storage.dashboards[i];
                var index = _.findIndex(dashboard.widgets, {id: id});
                if (index > -1) {
                    widget = dashboard.widgets[index];
                    break;
                }
            }
            return widget;
        }

        function setPanelLock(isLocked){
            service.gridsterOptions.draggable.enabled = !service.gridsterOptions.draggable.enabled;
            service.gridsterOptions.resizable.enabled = !service.gridsterOptions.resizable.enabled;
        }
        function setPanelMargins(margin){
            service.gridsterOptions.margins[0] = margin;
            service.gridsterOptions.margins[1] = margin;
        }

        function closeWidget(id) {
            for(var i = 0; i < service.storage.dashboards.length; i++) {
                var dashboard = service.storage.dashboards[i];
                var index = _.findIndex(dashboard.widgets, {id: id});
                if (index > -1) {
                    dashboard.widgets.splice(index, 1);
                    break;
                }
            }
        }
    }
})();
