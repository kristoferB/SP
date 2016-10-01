import { NgModule }      from '@angular/core';
import { NgGridModule } from 'angular2-grid';

import { WidgetComponent } from '../widget/widget.component';
import { DashboardComponent } from './dashboard.component';

@NgModule({
    imports: [
        NgGridModule
    ],
    declarations: [
        WidgetComponent,
        DashboardComponent
    ],
    bootstrap: [ DashboardComponent ]
})

export class DashboardModule { }

