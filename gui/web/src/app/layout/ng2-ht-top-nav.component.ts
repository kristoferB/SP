import { Component, Inject } from '@angular/core';

import { upgradeAdapter } from '../upgrade_adapter';

@Component({
  selector: 'ng2-ht-top-nav',
  templateUrl: 'app/layout/ht-top-nav.html',
  styleUrls: [],
  directives: [upgradeAdapter.upgradeNg1Component('upgUserDropdown'),
            upgradeAdapter.upgradeNg1Component('upgTopNavElements')],
  providers: []
})

export class Ng2HtTopNavComponent {

    showNavbar: boolean = false;
    togglePanelLock: void;
    toggleNavbar: void;

    constructor(@Inject('settingsService') settingsService) {
        this.showNavbar = settingsService.showNavbar;
        this.togglePanelLock = settingsService.togglePanelLock;
        this.toggleNavbar = settingsService.toggleNavbar;
    }

}
