import { Component, Inject } from '@angular/core';
import { DROPDOWN_DIRECTIVES } from 'ng2-bootstrap';

import { upgAdapter } from '../upg-helpers/upg-adapter';
import { Ng2DashboardService } from '../dashboard/ng2-dashboard.service';
import { WidgetKind } from '../widget-kind';
import { widgetKinds } from '../widget-kinds';

@Component({
  selector: 'sp-top-nav',
  templateUrl: 'app/layout/sp-top-nav.component.html',
  styleUrls: [],
  directives: [DROPDOWN_DIRECTIVES],
  providers: []
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

    constructor(
        @Inject('modelService') modelService,
        @Inject('dashboardService') dashboardService,
        @Inject('widgetListService') widgetListService,
        @Inject('$state') $state,
        @Inject('$uibModal') $uibModal,
        @Inject('themeService') themeService,
        @Inject('settingsService') settingsService,
        private ng2DashboardService: Ng2DashboardService
    ) {

        this.showNavbar = themeService.showNavbar;

        this.togglePanelLock = settingsService.togglePanelLock;

        this.showNavbar = true;
        //this.toggleNavbar = themeService.toggleNavbar; // implement it like this when themeService is ng2
        this.toggleNavbar = function() {
            this.showNavbar = !this.showNavbar;
            themeService.toggleNavbar();
        };

        this.activeModel = () => modelService.activeModel ?
            modelService.activeModel.name : null;

        this.isState = $state.is;

        this.createModel = function() {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/models/createmodel.html',
                controller: 'CreateModelController',
                controllerAs: 'vm'
            });

            modalInstance.result.then(function(chosenName) {
                modelService.createModel(chosenName);
            });
        };

        // upg-note: ugly custom resolve function will be changed when
        // widgetListService is rewritten and returns a proper Promise

        //var thiz = this;
        //widgetListService.list(function(list) {
        //   thiz.widgetKinds = list;
        //});
        this.widgetKinds = widgetKinds;

        this.addWidget = function(widgetKind: any) {
            ng2DashboardService.addWidget(
                ng2DashboardService.storage.dashboards[0], widgetKind
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
    }
}
