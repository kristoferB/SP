import { Component } from '@angular/core';

import { Ng2ShellComponent } from './ng2-shell.component';

@Component({
  selector: 'ng2-app',
  template: '<ng2-shell></ng2-shell>',
  styleUrls: [],
  directives: [Ng2ShellComponent],
  providers: []
})

export class Ng2AppComponent {
}
