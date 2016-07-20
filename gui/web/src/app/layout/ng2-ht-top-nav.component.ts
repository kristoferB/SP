import { Component, Inject } from '@angular/core';
import { DROPDOWN_DIRECTIVES } from 'ng2-bootstrap';

import { upgradeAdapter } from '../upgrade_adapter';

@Component({
  selector: 'ng2-ht-top-nav',
  templateUrl: 'app/layout/ht-top-nav.html',
  styleUrls: [],
  directives: [DROPDOWN_DIRECTIVES],
  providers: []
})

export class Ng2HtTopNavComponent {

    showNavbar: boolean;
    togglePanelLock: void;
    toggleNavbar: void;

    activeModel: () => string;
    createModel: () => void;
    isState: () => boolean;
    
    widgetKinds: any[];
    addWidget: (widgetKind: any) => void;
    widgetKindTitle: string;

    constructor(
        @Inject('modelService') modelService,
        @Inject('dashboardService') dashboardService,
        @Inject('widgetListService') widgetListService,
        @Inject('$state') $state,
        @Inject('$uibModal') $uibModal,
        @Inject('settingsService') settingsService
    ) {
        this.showNavbar = settingsService.showNavbar;
        this.togglePanelLock = settingsService.togglePanelLock;
        this.toggleNavbar = settingsService.toggleNavbar;

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
        }
        
        var thiz = this;
        widgetListService.list(function(list) {
           thiz.widgetKinds = list;
        }); 

        this.addWidget = function(widgetKind: any) { 
            dashboardService.addWidget(
                dashboardService.storage.dashboards[0], widgetKind
            );
        }
    }
}
