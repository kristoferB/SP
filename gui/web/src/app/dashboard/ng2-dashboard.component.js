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
var angular2_grid_1 = require('angular2-grid');
var upg_adapter_1 = require('../upg-helpers/upg-adapter');
var dcl_view_component_1 = require('./dcl-view.component');
var ng2_dashboard_service_1 = require('./ng2-dashboard.service');
var widget_kinds_1 = require('../widget-kinds');
var Ng2DashboardComponent = (function () {
    function Ng2DashboardComponent(logger, $state, 
        //@Inject('$timeout') $timeout,
        ng1DashboardService, ng2DashboardService) {
        var _this = this;
        this.ng2DashboardService = ng2DashboardService;
        ng2DashboardService.getDashboard(1, function (dashboard) {
            _this.dashboard = dashboard;
            _this.widgets = dashboard.widgets;
        });
        this.title = $state.current.title;
        this.ngGridOptions = ng2DashboardService.ngGridOptions;
        this.widgetKinds = widget_kinds_1.widgetKinds;
        this.togglePanelLock = function () {
            _this.ngGridOptions.draggable = !_this.ngGridOptions.draggable;
            _this.ngGridOptions.resizable = !_this.ngGridOptions.resizable;
            // vrf timeout??
            //$timeout( () => {
            //    this.gridsterOptions.draggable.enabled =
            //        !this.gridsterOptions.draggable.enabled;
            //    this.gridsterOptions.resizable.enabled =
            //        !this.gridsterOptions.resizable.enabled;
            //}, 500, false);
        };
    }
    Ng2DashboardComponent = __decorate([
        core_1.Component({
            selector: 'ng2-dashboard',
            templateUrl: 'app/dashboard/ng2-dashboard.component.html',
            styleUrls: [],
            directives: [angular2_grid_1.NgGrid, angular2_grid_1.NgGridItem, dcl_view_component_1.DclViewComponent,
                upg_adapter_1.upgAdapter.upgradeNg1Component('spWidget')],
            providers: [ng2_dashboard_service_1.Ng2DashboardService]
        }),
        __param(0, core_1.Inject('logger')),
        __param(1, core_1.Inject('$state')),
        __param(2, core_1.Inject('dashboardService')), 
        __metadata('design:paramtypes', [Object, Object, Object, ng2_dashboard_service_1.Ng2DashboardService])
    ], Ng2DashboardComponent);
    return Ng2DashboardComponent;
}());
exports.Ng2DashboardComponent = Ng2DashboardComponent;
//# sourceMappingURL=ng2-dashboard.component.js.map