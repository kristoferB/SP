import { Component } from '@angular/core';

@Component({
  selector: 'app',
  template: `
    <h1>{{title}}</h1>
  `,
  styleUrls: [],
  providers: []
})

export class AppComponent {
  title = 'Angular2 root app';
}
