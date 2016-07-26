import { Component, Inject } from '@angular/core';
import { NgGrid, NgGridItem } from 'angular2-grid';

import { upgAdapter } from '../upg-helpers/upg-adapter';
import { DclViewComponent } from './dcl-view.component';
import { Ng2DashboardService } from './ng2-dashboard.service';
import { WidgetKind } from '../widget-kind';
import { widgetKinds } from '../widget-kinds';


@Component({
  selector: 'ng2-dashboard',
  templateUrl: 'app/dashboard/ng2-dashboard.component.html',
  styleUrls: [],
  directives: [NgGrid, NgGridItem, DclViewComponent,
            upgAdapter.upgradeNg1Component('spWidget')],
  providers: [Ng2DashboardService]
})

export class Ng2DashboardComponent {

    dashboard: any;
    title: string;
    gridsterOptions: any;
    ngGridOptions: any;
    ngGridItemOptions: any;
    widgets: any[];
    widgetKinds: any[];
    togglePanelLock: () => void; // funkar ej än

    constructor(
        @Inject('logger') logger,
        @Inject('$state') $state,
        //@Inject('$timeout') $timeout,
        @Inject('dashboardService') ng1DashboardService,
        private ng2DashboardService: Ng2DashboardService
        ) {
            ng2DashboardService.getDashboard(1, (dashboard) => {
                this.dashboard = dashboard;
                this.widgets = dashboard.widgets;
            });
            this.title = $state.current.title;
            this.ngGridOptions = ng2DashboardService.ngGridOptions;
            this.widgetKinds = widgetKinds;
            this.togglePanelLock = () => {
                this.ngGridOptions.draggable = !this.ngGridOptions.draggable;
                this.ngGridOptions.resizable = !this.ngGridOptions.resizable;
                // vrf timeout??
                //$timeout( () => {
                //    this.gridsterOptions.draggable.enabled =
                //        !this.gridsterOptions.draggable.enabled;
                //    this.gridsterOptions.resizable.enabled =
                //        !this.gridsterOptions.resizable.enabled;
                //}, 500, false);
            }
        }
}
