import { Component, OnInit } from '@angular/core';

import { OpenWidget } from './open-widget';
import { OpenWidgetsService } from './open-widgets.service';

@Component({
  selector: 'sp-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})

export class DashboardComponent implements OnInit {

  ngGridOptions = {'max_cols': 6, 'auto_resize': true};
  openWidgets: OpenWidget[];

  constructor(
    openWidgetsService: OpenWidgetsService
  ) {
    this.openWidgets = openWidgetsService.openWidgets;
  }

  ngOnInit() {

  }
}



