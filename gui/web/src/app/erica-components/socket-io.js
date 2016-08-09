/**
 * Created by edvard on 2016-03-01.
 */
"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
//---------------- är detta en Singleton???
var core_1 = require('@angular/core');
var io = require('socket.io-client');
var SocketIO = (function () {
    function SocketIO() {
    }
    SocketIO.subscribe = function (eventType, onEventFunction) {
        this.connect(eventType);
        this.on(eventType, function (data) {
            onEventFunction(data);
            console.log('got data from ' + eventType + ', SocketIO!');
        });
    };
    SocketIO.connect = function (eventType) {
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
    };
    SocketIO.on = function (eventType, onEventFunction) {
        SocketIO.socket.on(eventType, onEventFunction);
    };
    SocketIO = __decorate([
        core_1.Injectable(), 
        __metadata('design:paramtypes', [])
    ], SocketIO);
    return SocketIO;
}());
exports.SocketIO = SocketIO;
//# sourceMappingURL=socket-io.js.map