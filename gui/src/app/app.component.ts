import { Component } from '@angular/core';
import { ApiService } from './shared'; // some shared stuff..

import '../style/app.scss';

@Component({
  selector: 'sp-app',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})

export class AppComponent {
  constructor(private api: ApiService) {
    // Do something with api
  }
}
