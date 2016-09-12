import { Component, Inject } from '@angular/core';
import { DROPDOWN_DIRECTIVES } from 'ng2-bootstrap';

import { upgAdapter } from '../upg-helpers/upg-adapter';
import { Ng2DashboardService } from '../dashboard/ng2-dashboard.service';
import { WidgetKind } from '../widget-kind';
import { widgetKinds } from '../widget-kinds';
import { ThemeService } from "../core/theme.service";

@Component({
    selector: 'sp-top-nav',
    templateUrl: 'app/layout/sp-top-nav.component.html',
    styleUrls: [],
    directives: [DROPDOWN_DIRECTIVES],
    providers: [],
    styles: []
})

export class SpTopNavComponent {

    showNavbar: boolean;
    togglePanelLock: void;
    toggleNavbar: () => void;

    activeModel: () => string;
    createModel: () => void;
    isState: () => boolean;

    widgetKinds: WidgetKind[];

    addWidget: (widgetKind: any) => void;
    widgetKindTitle: string;

    normalView: () => void;
    compactView: () => void;
    maximizedContentView: () => void;
    enableEditorMode: () => void;
    disableEditorMode: () => void;

    models: any;
    setActiveModel: (model: any) => void;
    activeModelName: () => string;

    createDashboard: () => void;

    dashboards: any[];
    setActiveDashboard: (dashboard: any) => void;
    activeDashboardName: () => string;

    currentColor: string;
    availableColors: string[];
    setCurrentColor: (color: string) => void;

    constructor(
        @Inject('modelService') modelService,
        @Inject('dashboardService') dashboardService,
        @Inject('widgetListService') widgetListService,
        @Inject('$state') $state,
        @Inject('$uibModal') $uibModal,
        @Inject('settingsService') settingsService,
        ng2DashboardService: Ng2DashboardService,
        themeService: ThemeService
    ) {
        this.currentColor = "default_white";
        this.availableColors = ["default_white", "blue", "dark", "happy"];
        this.setCurrentColor = themeService.setColorTheme;

        this.dashboards = ng2DashboardService.storage.dashboards;
        this.setActiveDashboard = ng2DashboardService.setActiveDashboard;

        this.showNavbar = themeService.showNavbar;

        this.togglePanelLock = settingsService.togglePanelLock;

        this.showNavbar = true;
        this.toggleNavbar = function () {
            this.showNavbar = !this.showNavbar;
            themeService.toggleNavbar();
        };

        this.activeModel = () => modelService.activeModel ?
            modelService.activeModel.name : null;

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

        this.createDashboard = function () {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/dashboard/createdashboard.html',
                controller: 'CreateDashboardController',
                controllerAs: 'vm'
            });

            modalInstance.result.then(function (chosenName) {
                ng2DashboardService.addDashboard(chosenName);
            });
        };


        // upg-note: ugly custom resolve function will be changed when
        // widgetListService is rewritten and returns a proper Promise

        //var thiz = this;
        //widgetListService.list(function(list) {
        //   thiz.widgetKinds = list;
        //});
        this.widgetKinds = widgetKinds;

        this.addWidget = function (widgetKind: any) {
            ng2DashboardService.addWidget(
                ng2DashboardService.activeDashboard, widgetKind
            );
        };

        this.normalView = themeService.normalView;
        this.compactView = themeService.compactView;
        this.maximizedContentView = themeService.maximizedContentView;
        this.enableEditorMode = themeService.enableEditorMode;
        this.disableEditorMode = themeService.disableEditorMode;

        this.models = modelService.models;
        this.setActiveModel = modelService.setActiveModel;
        this.activeModelName = () => modelService.activeModel ?
            modelService.activeModel.name : null;

        //this.activeDashboardName = () => ng2DashboardService.activeDashboardIndex ?
        //    ng2DashboardService.activeDashboardIndex : null;
        this.activeDashboardName = () => "placeholder";
    }
}

