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
var Ng2DashboardService = (function () {
    function Ng2DashboardService($sessionStorage, logger, $ocLazyLoad) {
        var _this = this;
        console.log('I got created!!!!!!!!!!!!1');
        console.log(widget_kinds_1.widgetKinds);
        this.storage = $sessionStorage.$default({
            dashboards: [{
                    id: 1,
                    name: 'My Board',
                    widgets: [] // borttaget: requiredFiles[]
                }],
            widgetID: 1,
            dashboardID: 2
        });
        this.addDashboard = function (name) {
            var dashboard = {
                id: _this.storage.dashboardID++,
                name: name,
                widgets: []
            };
            // title changed to name here
            logger.info('Dashboard Controller: Added a dashboard with name ' + dashboard.name + ' and index '
                + dashboard.id + '.');
        };
        this.getDashboard = function (id, callback) {
            //var index = _.findIndex(this.storage.dashboards, {id: id});
            var index = _this.storage.dashboards
                .map(function (x) { return x.id; }).indexOf(id);
            if (index === -1) {
                return null;
            }
            else {
                var dashboard = _this.storage.dashboards[index];
                callback(dashboard);
            }
        };
        this.removeDashboard = function (id) {
            //var index = _.findIndex(service.storage.dashboards, {id: id});
            var index = _this.storage.dashboards
                .map(function (x) { return x.id; }).indexOf(id);
            _this.storage.dashboards.splice(index, 1);
        };
        this.addWidget = function (dashboard, widgetKind) {
            //var widget = angular.copy(widgetKind, {});
            var widget = widgetKind; // TODO copy problems??
            widget.id = _this.storage.widgetID++;
            //needed??
            //if (additionalData !== undefined) {
            //    widget.storage = additionalData;
            //}
            dashboard.widgets.push(widget);
            logger.log('Dashboard Controller: Added a ' + widget.title + ' widget with index '
                + widget.id + ' to dashboard ' + dashboard.name + '.');
        };
        this.getWidget = function (id) {
            var widget = null;
            for (var i = 0; i < _this.storage.dashboards.length; i++) {
                var dashboard = _this.storage.dashboards[i];
                //var index = _.findIndex(dashboard.widgets, {id: id});
                var index = _this.storage.dashboards
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
            _this.ngGridOptions.margins = margin;
        };
        this.closeWidget = function (id) {
            for (var i = 0; i < _this.storage.dashboards.length; i++) {
                var dashboard = _this.storage.dashboards[i];
                //var index = _.findIndex(dashboard.widgets, {id: id});
                var index = _this.storage.dashboards
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
            'auto_resize': true,
            'maintain_ratio': false,
            'max_cols': 12
        };
        this.ngGridItemOptions = {
            'col': 4,
            'row': 4,
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
//# sourceMappingURL=ng2-dashboard.service.js.map