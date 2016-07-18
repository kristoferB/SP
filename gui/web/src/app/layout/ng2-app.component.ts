import { Component, Inject } from '@angular/core';

import { upgradeAdapter } from '../upgrade_adapter';

@Component({
  selector: 'ng2-app',
  template: '<h1> {{ title }} </h1><shell>gablargh</shell>',
  styleUrls: [],
  directives: [upgradeAdapter.upgradeNg1Component('shell')],
  providers: []
})

export class Ng2AppComponent {
  title = 'Angular2 root app';

  constructor() {
      this.title = 'eller är det?';
  }
}
