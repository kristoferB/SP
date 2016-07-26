import { Injectable, Inject } from '@angular/core';

import { WidgetKind } from '../widget-kind';
import { widgetKinds } from '../widget-kinds';

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
    setPanelMargins: (margin: number) => void; // typat ok?
    ngGridOptions: any; // TODO typa till key-value?
    ngGridItemOptions: any;
    widgetKinds: any; // ska lösas på något sätt

    constructor(
        @Inject('$sessionStorage') $sessionStorage,
        @Inject('logger') logger,
        @Inject('$ocLazyLoad') $ocLazyLoad
    ) {
        console.log('I got created!!!!!!!!!!!!1');
        console.log(widgetKinds);

        this.storage = $sessionStorage.$default({
            dashboards: [{
                id: 1,
                name: 'My Board',
                widgets: [] // borttaget: requiredFiles[]
            }],
            widgetID: 1,
            dashboardID: 2
        })

        this.addDashboard = (name) => {
            var dashboard = {
                id: this.storage.dashboardID++,
                name: name,
                widgets: []
            };
            // title changed to name here
            logger.info('Dashboard Controller: Added a dashboard with name ' + dashboard.name + ' and index '
                + dashboard.id + '.');
        }

        this.getDashboard = (id, callback) => {
            //var index = _.findIndex(this.storage.dashboards, {id: id});
            var index = this.storage.dashboards
                        .map( (x) => x.id ).indexOf(id);
            if (index === -1) {
                return null
            } else {
                var dashboard = this.storage.dashboards[index];
                callback(dashboard);
            }
        }

        this.removeDashboard = (id) => {
            //var index = _.findIndex(service.storage.dashboards, {id: id});
            var index = this.storage.dashboards
                        .map( (x) => x.id ).indexOf(id);
            this.storage.dashboards.splice(index, 1);
        }

        this.addWidget = (dashboard, widgetKind) => {

            //var widget = angular.copy(widgetKind, {});
            var widget = widgetKind; // TODO copy problems??
            widget.id = this.storage.widgetID++;
            //needed??
            //if (additionalData !== undefined) {
            //    widget.storage = additionalData;
            //}
            dashboard.widgets.push(widget);
            logger.log('Dashboard Controller: Added a ' + widget.title + ' widget with index '
                + widget.id + ' to dashboard ' + dashboard.name + '.');
        }

        this.getWidget = (id) => {
            var widget = null;
            for(var i = 0; i < this.storage.dashboards.length; i++) {
                var dashboard = this.storage.dashboards[i];
                //var index = _.findIndex(dashboard.widgets, {id: id});
                var index = this.storage.dashboards
                            .map( (x) => x.id ).indexOf(id);
                if (index > -1) {
                    widget = dashboard.widgets[index];
                    break;
                }
            }
            return widget;
        }

        this.setPanelLock = (isLocked) => {
            this.ngGridOptions.resizable = isLocked;
            this.ngGridOptions.draggable = isLocked;
        }

        this.setPanelMargins = (margin) => {
            this.ngGridOptions.margins = margin;
        }

        this.closeWidget = (id) => {
            for(var i = 0; i < this.storage.dashboards.length; i++) {
                var dashboard = this.storage.dashboards[i];
                //var index = _.findIndex(dashboard.widgets, {id: id});
                var index = this.storage.dashboards
                            .map( (x) => x.id ).indexOf(id);
                if (index > -1) {
                    dashboard.widgets.splice(index, 1);
                    break;
                }
            }
        }

        this.ngGridOptions = {
            'resizable': true,
            'draggable': true,
            'margins': [10],
            'auto_resize': true,
            'maintain_ratio': false,
            'max_cols': 12
        }

        this.ngGridItemOptions = {
            'col': 4,
            'row': 4,
            'fixed': true,
            'dragHandle': null,
            'borderSize': 15, // default
            'resizeHandle': null
        }
    }

}
