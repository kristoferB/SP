/**
 * Created by edvard on 2016-03-01.
 */

//---------------- är detta en Singleton???

import { Injectable } from '@angular/core';

import * as io from 'socket.io-client';

@Injectable()
export class SocketIO {
    private static socket: any;

    public static subscribe(eventType: string, onEventFunction: (eventData: any) => void) {
        this.connect(eventType);
        this.on(eventType, function(data) {
            onEventFunction(data);
            console.log('got data from ' + eventType + ', SocketIO!');
        });
    }

    public static connect(eventType: string) {
        if (this.socket != null) {
          console.log('request response!');
          this.socket.emit('eventType', eventType);
          return;
        }
        // TODO hårdkodat, hur lösa
        this.socket = io.connect('http://localhost:8000');
        this.on('connectionResponse', function (d) {
            console.log('server responded: ' + d + "!");
            SocketIO.socket.emit('eventType', eventType);
        });
    }

    public static on(eventType: string, onEventFunction: (eventData: any) => void) {
        SocketIO.socket.on(eventType, onEventFunction);
    }
}
