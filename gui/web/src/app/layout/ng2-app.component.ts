import { Component, Inject } from '@angular/core';

import { upgradeAdapter } from '../upgrade_adapter';
import { Ng2ShellComponent } from './ng2-shell.component';

@Component({
  selector: 'ng2-app',
  template: '<h1> {{ title }} </h1><ng2-shell></ng2-shell>',
  styleUrls: [],
  directives: [Ng2ShellComponent],
  providers: []
})

export class Ng2AppComponent {
  title = 'Angular2 root app';

  constructor() {
      this.title = 'eller är det?';
  }
}
