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
var ng2_bootstrap_1 = require('ng2-bootstrap');
var ng2_dashboard_service_1 = require('../dashboard/ng2-dashboard.service');
var widget_kinds_1 = require('../widget-kinds');
var SpTopNavComponent = (function () {
    function SpTopNavComponent(modelService, dashboardService, widgetListService, $state, $uibModal, themeService, settingsService, ng2DashboardService) {
        this.ng2DashboardService = ng2DashboardService;
        this.showNavbar = themeService.showNavbar;
        this.togglePanelLock = settingsService.togglePanelLock;
        this.showNavbar = true;
        //this.toggleNavbar = themeService.toggleNavbar; // implement it like this when themeService is ng2
        this.toggleNavbar = function () {
            this.showNavbar = !this.showNavbar;
            themeService.toggleNavbar();
        };
        this.activeModel = function () { return modelService.activeModel ?
            modelService.activeModel.name : null; };
        this.isState = $state.is;
        this.createModel = function () {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/models/createmodel.html',
                controller: 'CreateModelController',
                controllerAs: 'vm'
            });
            modalInstance.result.then(function (chosenName) {
                modelService.createModel(chosenName);
            });
        };
        // upg-note: ugly custom resolve function will be changed when
        // widgetListService is rewritten and returns a proper Promise
        //var thiz = this;
        //widgetListService.list(function(list) {
        //   thiz.widgetKinds = list;
        //});
        this.widgetKinds = widget_kinds_1.widgetKinds;
        this.addWidget = function (widgetKind) {
            ng2DashboardService.addWidget(ng2DashboardService.storage.dashboards[0], widgetKind);
        };
        this.normalView = themeService.normalView;
        this.compactView = themeService.compactView;
        this.maximizedContentView = themeService.maximizedContentView;
        this.enableEditorMode = themeService.enableEditorMode;
        this.disableEditorMode = themeService.disableEditorMode;
        this.models = modelService.models;
        this.setActiveModel = modelService.setActiveModel;
        this.activeModelName = function () { return modelService.activeModel ?
            modelService.activeModel.name : null; };
    }
    SpTopNavComponent = __decorate([
        core_1.Component({
            selector: 'sp-top-nav',
            templateUrl: 'app/layout/sp-top-nav.component.html',
            styleUrls: [],
            directives: [ng2_bootstrap_1.DROPDOWN_DIRECTIVES],
            providers: []
        }),
        __param(0, core_1.Inject('modelService')),
        __param(1, core_1.Inject('dashboardService')),
        __param(2, core_1.Inject('widgetListService')),
        __param(3, core_1.Inject('$state')),
        __param(4, core_1.Inject('$uibModal')),
        __param(5, core_1.Inject('themeService')),
        __param(6, core_1.Inject('settingsService')), 
        __metadata('design:paramtypes', [Object, Object, Object, Object, Object, Object, Object, ng2_dashboard_service_1.Ng2DashboardService])
    ], SpTopNavComponent);
    return SpTopNavComponent;
}());
exports.SpTopNavComponent = SpTopNavComponent;
//# sourceMappingURL=sp-top-nav.component.js.map