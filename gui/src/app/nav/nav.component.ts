import { Component } from '@angular/core';

import { Menu, MenuButton } from './menu';

@Component({
  selector: 'sp-nav',
  templateUrl: './nav.component.html'
})
export class NavComponent {

  buttons: MenuButton[] = [
    {
      'action': () => { console.log('action!'); },
      'text': 'teeext',
      'symbol': 'stjöstjärna'
    },
    {
      'action': () => { console.log('called theOtherOne'); },
      'text': 'abcdfg',
      'symbol': 'fisk'
    }
  ];
  menu1: Menu = { 'text': 'Kristofer', 'symbol': '2', 'buttons': this.buttons };
  menu2: Menu = { 'text': 'settings', 'symbol': 'inge', 'buttons': this.buttons };
  menus: Menu[] = [this.menu1, this.menu2];

  // The job of this constructor is assigning service-
  // functions to MenuButtons, no logic is allowed in here
  constructor() { }
}
