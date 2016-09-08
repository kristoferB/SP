import { Component, Inject, NgZone } from '@angular/core';
import { NgGrid, NgGridItem } from 'angular2-grid';

import { upgAdapter } from '../upg-helpers/upg-adapter';
import { DclViewComponent } from './dcl-view.component';
import { Ng2DashboardService } from './ng2-dashboard.service';
import { widgetKinds } from '../widget-kinds';
import { ThemeService } from "../core/theme.service";

import { DOCUMENT } from '@angular/platform-browser';


@Component({
  selector: 'ng2-dashboard',
  templateUrl: 'app/dashboard/ng2-dashboard.component.html',
  styleUrls: [],
  directives: [NgGrid, NgGridItem, DclViewComponent, upgAdapter.upgradeNg1Component('spWidget')],
  providers: []
})

export class Ng2DashboardComponent {

    dashboard: any;
    title: string;
    gridsterOptions: any;
    ngGridOptions: any;
    widgets: any[];
    widgetKinds: any[];
    togglePanelLock: () => void; 
    getDashboard: () => any;

    constructor(
        @Inject('logger') logger,
        @Inject('$state') $state,
        private ng2DashboardService: Ng2DashboardService,
        private themeService: ThemeService
    ) {

        // initialize
        this.dashboard = ng2DashboardService.activeDashboard;
        this.widgets = this.dashboard.widgets;

        ng2DashboardService.dashboardChanged.subscribe(
            (dashboard: any) => {
                console.log("changed to: "+ dashboard.name);
                this.dashboard = ng2DashboardService.activeDashboard;
                this.widgets = this.dashboard.widgets;
            }
        );

        this.title = $state.current.title;

        this.widgetKinds = widgetKinds;

        this.togglePanelLock = () => {
            this.ngGridOptions.draggable = !this.ngGridOptions.draggable;
            this.ngGridOptions.resizable = !this.ngGridOptions.resizable;
        };
    }
}

