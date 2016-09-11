import { Component, Input, Inject } from '@angular/core';

import { ThemeService } from "../core/theme.service";
import { Ng2DashboardService } from '../dashboard/ng2-dashboard.service';

@Component({
  selector: 'ng2-sp-widget',
  templateUrl: 'app/ng2-sp-widget/ng2-sp-widget.component.html'
})

export class Ng2SpWidgetComponent {

    @Input() widget: any;
    @Input() dashboard: any;
    @Input() showCloseBtn: boolean;

    settingsService: any;
    ng2DashboardService: any;
    themeService: any;

    requestClose: (widgetId: number) => void;

    constructor(
        @Inject('settingsService') settingsService,
        ng2DashboardService: Ng2DashboardService,
        themeService: ThemeService
    ) {
        this.settingsService = settingsService;
        this.themeService = themeService;
        this.requestClose = (widgetId) => {
            ng2DashboardService.closeWidget(widgetId);
        };
    }
}
