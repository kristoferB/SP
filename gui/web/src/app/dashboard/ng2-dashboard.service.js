"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
var core_1 = require('@angular/core');
var widget_kinds_1 = require('../widget-kinds');
var Rx_1 = require("rxjs/Rx");
var Ng2DashboardService = (function () {
    function Ng2DashboardService($sessionStorage, logger, $ocLazyLoad) {
        var _this = this;
        this.dashboardChangedSubject = new Rx_1.Subject();
        this.dashboardChanged = this.dashboardChangedSubject.asObservable();
        this.storage = $sessionStorage.$default({
            dashboards: [
                new Dashboard(0, 'My Board', []),
                new Dashboard(1, 'Other board', [])
            ],
            widgetID: 1,
            dashboardID: 2
        });
        this.setActiveDashboard = function (dashboard) {
            _this.activeDashboard = dashboard;
            _this.dashboardChangedSubject.next('this should not be a string but rather the dashboard itself');
        };
        this.setActiveDashboard(this.storage.dashboards[0]);
        this.addDashboard = function (name) {
            var dashboard = new Dashboard(_this.storage.dashboardID++, name, []);
            _this.storage.dashboards.push(dashboard);
            logger.info('Dashboard Controller: Added a dashboard with name ' + dashboard.name + ' and index '
                + dashboard.id + '.');
        };
        this.removeDashboard = function (id) {
            var index = _this.storage.dashboards
                .map(function (x) { return x.id; }).indexOf(id);
            _this.storage.dashboards.splice(index, 1);
        };
        this.addWidget = function (dashboard, widgetKind) {
            var index = widget_kinds_1.widgetKinds.indexOf(widgetKind);
            var widget = Object.create(widgetKind);
            widget.index = index;
            widget.id = _this.storage.widgetID++;
            //needed??
            //if (additionalData !== undefined) {
            //    widget.storage = additionalData;
            //}
            dashboard.widgets.push(widget);
            widget.gridOptions = Object.create(ngGridItemOptionDefaults);
            logger.log('Dashboard Controller: Added a ' + widget.title + ' widget with index '
                + widget.id + ' to dashboard ' + dashboard.name + '.');
        };
        this.getWidget = function (id) {
            var widget = null;
            for (var i = 0; i < _this.storage.dashboards.length; i++) {
                var dashboard = _this.storage.dashboards[i];
                //var index = _.findIndex(dashboard.widgets, {id: id});
                var index = dashboard.widgets
                    .map(function (x) { return x.id; }).indexOf(id);
                if (index > -1) {
                    widget = dashboard.widgets[index];
                    break;
                }
            }
            return widget;
        };
        this.setPanelLock = function (isLocked) {
            _this.ngGridOptions.resizable = isLocked;
            _this.ngGridOptions.draggable = isLocked;
        };
        this.setPanelMargins = function (margin) {
            _this.ngGridOptions.margins = [margin];
        };
        this.closeWidget = function (id) {
            for (var i = 0; i < _this.storage.dashboards.length; i++) {
                var dashboard = _this.storage.dashboards[i];
                var index = dashboard.widgets
                    .map(function (x) { return x.id; }).indexOf(id);
                if (index > -1) {
                    dashboard.widgets.splice(index, 1);
                    break;
                }
            }
        };
        this.ngGridOptions = {
            'resizable': true,
            'draggable': true,
            'margins': [10],
            'auto_resize': false,
            'maintain_ratio': false,
            'col_width': window.innerWidth / 12,
            'row_height': (window.innerHeight - 50) / 8
        };
        var ngGridItemOptionDefaults = {
            'col': 1,
            'row': 1,
            'fixed': true,
            'dragHandle': null,
            'borderSize': 15,
            'resizeHandle': null
        };
    }
    Ng2DashboardService = __decorate([
        core_1.Injectable(),
        __param(0, core_1.Inject('$sessionStorage')),
        __param(1, core_1.Inject('logger')),
        __param(2, core_1.Inject('$ocLazyLoad')), 
        __metadata('design:paramtypes', [Object, Object, Object])
    ], Ng2DashboardService);
    return Ng2DashboardService;
}());
exports.Ng2DashboardService = Ng2DashboardService;
var Dashboard = (function () {
    function Dashboard(id, name, widgets) {
        this.id = id;
        this.name = name;
        this.widgets = widgets;
    }
    return Dashboard;
}());
exports.Dashboard = Dashboard;
//# sourceMappingURL=ng2-dashboard.service.js.map