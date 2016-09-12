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
var theme_service_1 = require("../core/theme.service");
var ng2_dashboard_service_1 = require('../dashboard/ng2-dashboard.service');
var Ng2SpWidgetComponent = (function () {
    function Ng2SpWidgetComponent(settingsService, ng2DashboardService, themeService) {
        this.settingsService = settingsService;
        this.themeService = themeService;
        this.requestClose = function (widgetId) {
            ng2DashboardService.closeWidget(widgetId);
        };
    }
    __decorate([
        core_1.Input(), 
        __metadata('design:type', Object)
    ], Ng2SpWidgetComponent.prototype, "widget", void 0);
    __decorate([
        core_1.Input(), 
        __metadata('design:type', Object)
    ], Ng2SpWidgetComponent.prototype, "dashboard", void 0);
    __decorate([
        core_1.Input(), 
        __metadata('design:type', Boolean)
    ], Ng2SpWidgetComponent.prototype, "showCloseBtn", void 0);
    Ng2SpWidgetComponent = __decorate([
        core_1.Component({
            selector: 'ng2-sp-widget',
            templateUrl: 'app/ng2-sp-widget/ng2-sp-widget.component.html'
        }),
        __param(0, core_1.Inject('settingsService')), 
        __metadata('design:paramtypes', [Object, ng2_dashboard_service_1.Ng2DashboardService, theme_service_1.ThemeService])
    ], Ng2SpWidgetComponent);
    return Ng2SpWidgetComponent;
}());
exports.Ng2SpWidgetComponent = Ng2SpWidgetComponent;
//# sourceMappingURL=ng2-sp-widget.component.js.map