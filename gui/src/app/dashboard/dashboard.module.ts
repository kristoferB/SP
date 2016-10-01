import { NgModule }      from '@angular/core';
import { NgGridModule } from 'angular2-grid';

import { WidgetFrameComponent } from '../widget-frame/widget-frame.component';
import { DashboardComponent } from './dashboard.component';

@NgModule({
    imports: [
        NgGridModule
    ],
    declarations: [
        WidgetFrameComponent,
        DashboardComponent
    ],
    bootstrap: [ DashboardComponent ]
})

export class DashboardModule { }

