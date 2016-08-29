(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .factory('dashboardService', dashboardService);

    dashboardService.$inject = ['$sessionStorage', 'logger', '$ocLazyLoad', 'widgetListService'];
    /* @ngInject */
    function dashboardService($sessionStorage, logger, $ocLazyLoad, widgetListService) {
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
                    widgets: [],
                    requiredFiles: []
                }],
                widgetID: 1,
                dashboardID: 2
            }),

            setPanelLock: setPanelLock,
            setPanelMargins: setPanelMargins,
            gridsterOptions : {
                outerMargin: false,
                columns: 12,
                rowHeight: (window.innerHeight-50) / 8,
                margins: [0,0],
                floating: false,
                pushing: false,
                swapping: false,
                draggable: {
                    enabled: false,
                    handle: '.panel-default'
                },
                resizable:{
                    enabled: false
                }
            },
            ngGridOptions: {
                'resizable': false,
                'margins': [0],
                'auto_resize': true,
                'maintain_ratio': true,
                'max_cols': 12
            }
        };

        // asynchronicity doesn't cause a problem, verifiable with
        // setTimeout(function() {service.widgetKinds = list;}, 10000);
        // menu-items will be viewable after those 10 seconds
        widgetListService.list(function(list) {
            service.widgetKinds = list;
        });

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

        function getDashboard(id, callback) {
            var index = _.findIndex(service.storage.dashboards, {id: id});
            if (index === -1) {
                return null
            } else {
                var dashboard = service.storage.dashboards[index];
                $ocLazyLoad.load(dashboard.requiredFiles).then(function() {
                    callback(dashboard);
                });
            }
        }

        function removeDashboard(id) {
            var index = _.findIndex(service.storage.dashboards, {id: id});
            service.storage.dashboards.splice(index, 1);
        }

        function addWidget(dashboard, widgetKind, additionalData) {

            var requiredFiles = widgetKind.jsfiles;
            for (var index in requiredFiles) {
                console.log(requiredFiles[index]);
                dashboard.requiredFiles.push(requiredFiles[index]);
            }
            $ocLazyLoad.load(requiredFiles).then(function() {
                var widget = angular.copy(widgetKind, {});
                widget.id = service.storage.widgetID++;
                if (additionalData !== undefined) {
                    widget.storage = additionalData;
                }
                dashboard.widgets.push(widget);
                logger.log('Dashboard Controller: Added a ' + widget.title + ' widget with index '
                    + widget.id + ' to dashboard ' + dashboard.name + '.');
            });
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
            service.gridsterOptions.draggable.enabled = isLocked;
            service.gridsterOptions.resizable.enabled = isLocked;
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
