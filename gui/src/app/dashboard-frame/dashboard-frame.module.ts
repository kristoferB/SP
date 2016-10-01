import { NgModule }      from '@angular/core';

import { WidgetFrameComponent } from '../widget-frame/widget-frame.component';
import { DashboardFrameComponent } from './dashboard-frame.component';
import { NgGridModule } from 'angular2-grid';

@NgModule({
    imports: [
        NgGridModule
    ],
    declarations: [
        WidgetFrameComponent,
        DashboardFrameComponent
    ],
    bootstrap: [ DashboardFrameComponent ]
})

export class DashboardFrameModule { }

