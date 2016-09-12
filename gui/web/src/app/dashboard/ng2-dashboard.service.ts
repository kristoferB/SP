import {Injectable, Inject}  from '@angular/core';

import { WidgetKind } from '../widget-kind';
import { widgetKinds } from '../widget-kinds';
import { Subject, Observable } from "rxjs/Rx";

@Injectable()
export class Ng2DashboardService {
    addDashboard: (name: string) => void;
    getDashboard: (id: number, callback: (dashboard: any) => void) => any; // TODO typa
    removeDashboard: (id: number) => void;
    addWidget: (dashboard: any, widget: WidgetKind) // additional data??
                => void; // TODO parameter ok?
    getWidget: (id: number) => WidgetKind; // typat ok?
    closeWidget: (id: number) => void;
    storage: any; // TODO typa
    setPanelLock: (isLocked: boolean) => void;
    setPanelMargins: (margin: number) => void;
    ngGridOptions: any; // TODO typa till key-value?

    widgetKinds: any; // ska lösas på något sätt
    setActiveDashboard: (dashboard: any) => any;
    activeDashboard: any;

    dashboardChangedSubject: Subject<any>;
    dashboardChanged: Observable<any>;
    createDashboard: (name: string) => void;

    constructor(
        @Inject('$sessionStorage') $sessionStorage,
        @Inject('logger') logger,
        @Inject('$ocLazyLoad') $ocLazyLoad
    ) {
        this.dashboardChangedSubject = new Subject<any>();
        this.dashboardChanged = this.dashboardChangedSubject.asObservable();

        this.storage = $sessionStorage.$default({
            dashboards: [
                new Dashboard(0, 'My Board', []),
                new Dashboard(1, 'Other board', [])
            ],
            widgetID: 1,
            dashboardID: 2
        });

        this.setActiveDashboard = (dashboard: any) => {
            this.activeDashboard = dashboard;
            this.dashboardChangedSubject.next('this should not be a string but rather the dashboard itself');
        };
        this.setActiveDashboard(this.storage.dashboards[0]);

        this.addDashboard = (name) => {
            var dashboard = new Dashboard(
                this.storage.dashboardID++,
                name,
                []
            );


            this.storage.dashboards.push(dashboard);

            logger.info('Dashboard Controller: Added a dashboard with name ' + dashboard.name + ' and index '
                + dashboard.id + '.');
        };


        this.removeDashboard = (id) => {
            var index = this.storage.dashboards
                        .map( (x) => x.id ).indexOf(id);
            this.storage.dashboards.splice(index, 1);
        };

        this.addWidget = (dashboard, widgetKind) => {

            var index = widgetKinds.indexOf(widgetKind);
            var widget = Object.create(widgetKind);
            widget.index = index;
            widget.id = this.storage.widgetID++;
            //needed??
            //if (additionalData !== undefined) {
            //    widget.storage = additionalData;
            //}
            dashboard.widgets.push(widget);
            widget.gridOptions = Object.create(ngGridItemOptionDefaults);

            logger.log('Dashboard Controller: Added a ' + widget.title + ' widget with index '
                + widget.id + ' to dashboard ' + dashboard.name + '.');
        };

        this.getWidget = (id) => {
            var widget = null;
            for(var i = 0; i < this.storage.dashboards.length; i++) {
                var dashboard = this.storage.dashboards[i];
                //var index = _.findIndex(dashboard.widgets, {id: id});
                var index = dashboard.widgets
                            .map( (x) => x.id ).indexOf(id);
                if (index > -1) {
                    widget = dashboard.widgets[index];
                    break;
                }
            }
            return widget;
        };

        this.setPanelLock = (isLocked) => {
            this.ngGridOptions.resizable = isLocked;
            this.ngGridOptions.draggable = isLocked;
        };

        this.setPanelMargins = (margin) => {
            this.ngGridOptions.margins = [margin];
        };

        this.closeWidget = (id) => {
            for(var i = 0; i < this.storage.dashboards.length; i++) {
                var dashboard = this.storage.dashboards[i];
                var index = dashboard.widgets
                            .map( (x) => x.id ).indexOf(id);
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
            'auto_resize': false,
            'maintain_ratio': false,
    	    'col_width':window.innerWidth/12,
            'row_height':(window.innerHeight-50) / 8
        };

        var ngGridItemOptionDefaults = {
            'col': 1,
            'row': 1,
            'fixed': true,
            'dragHandle': null,
            'borderSize': 15, // default
            'resizeHandle': null
        };
    }
}

export class Dashboard {
    id: number;
    name: string;
    widgets: any[];
    constructor(id, name, widgets){
        this.id = id;
        this.name = name;
        this.widgets = widgets;
    }
}
