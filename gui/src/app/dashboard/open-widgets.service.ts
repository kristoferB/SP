import { Injectable }  from '@angular/core';

import { OpenWidget } from './open-widget';

@Injectable()
export class OpenWidgetsService {

  openWidgets: OpenWidget[] = [
    {
      text: 'här e en öppen wid',
      ngGridItemOptions: {sizex: 1, sizey: 1}
    },
    {
      text: 'här e en till',
      ngGridItemOptions: {sizex: 1, sizey: 1}
    }
  ];

}
