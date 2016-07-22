import { Component, Inject } from '@angular/core';
import { NgGrid, NgGridItem } from 'angular2-grid';

import { upgAdapter } from '../upg-helpers/upg-adapter';

@Component({
  selector: 'ng2-dashboard',
  templateUrl: 'app/dashboard/ng2-dashboard.component.html',
  styleUrls: [],
  directives: [NgGrid, NgGridItem,
            upgAdapter.upgradeNg1Component('spWidget')],
  providers: []
})

export class Ng2DashboardComponent {

    dashboard: any;
    title: string;
    gridsterOptions: any;
    ngGridOptions: any;
    widgets: any[];
    togglePanelLock: () => void; // funkar ej än

    constructor(
        @Inject('logger') logger,
        @Inject('$state') $state,
        //@Inject('$timeout') $timeout,
        @Inject('dashboardService') dashboardService
        ) {
            dashboardService.getDashboard(1, (dashboard) => {
                this.dashboard = dashboard;
                this.widgets = dashboard.widgets;
            });
            this.title = $state.current.title;
            this.gridsterOptions = dashboardService.gridsterOptions;
            this.ngGridOptions = dashboardService.ngGridOptions;
            //this.togglePanelLock = () => {
            //    $timeout( () => {
            //        this.gridsterOptions.draggable.enabled =
            //            !this.gridsterOptions.draggable.enabled;
            //        this.gridsterOptions.resizable.enabled =
            //            !this.gridsterOptions.resizable.enabled;
            //    }, 500, false);
            //}
        }
}
