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
}
