import { NgModule }      from '@angular/core';
import { NgGridModule } from 'angular2-grid';
import { BrowserModule } from '@angular/platform-browser';

import { WidgetComponent } from '../widget/widget.component';
import { DashboardComponent } from './dashboard.component';
import { OpenWidgetsService } from './open-widgets.service';

@NgModule({
  imports: [
    NgGridModule,
    BrowserModule
  ],
  declarations: [
    WidgetComponent,
    DashboardComponent
  ],
  providers: [
    OpenWidgetsService
  ],
  bootstrap: [ DashboardComponent ]
})

export class DashboardModule { }

