/**
 * Created by oskar on 06/09/16.
 *
 *
 * To use in a class:
 * class name {
 *     private eventBus:EventBusService;
 *     constructor( eventBus:EventBusService){
 *       this.eventBus = eventBus;
 *   }
 *
 *   laterOnFunction(){
 *   eventBus.testToSubscribe();
 *   }
 *
 *
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
var core_1 = require('@angular/core');
var EventBusService = (function () {
    function EventBusService() {
        this.topics = {};
    }
    EventBusService.prototype.testToSubscribe = function () {
        var _this = this;
        console.log("trying out the event-bus");
        this.subscribeToTopic("test", function (bus) {
            if (bus === void 0) { bus = _this; }
            confirmed(bus);
        }, callback);
        function confirmed(busService) {
            console.log("subscribtion confirmed!");
            busService.tweetToTopic("test", "hejhej!");
        }
        function callback(data2) {
            console.log("callback");
            console.log(data2);
        }
    };
    EventBusService.prototype.subscribeToTopic = function (name, confirmFunc, func) {
        var topic = this.getTopic(name);
        topic.subscribe(func);
        confirmFunc();
    };
    EventBusService.prototype.unsubscribeToTopic = function (name, func) {
        var topic = this.getTopic(name);
        topic.unsubscribe(func);
    };
    EventBusService.prototype.tweetToTopic = function (name, args) {
        var topic = this.getTopic(name);
        topic.tweet(args);
    };
    //privates
    EventBusService.prototype.getTopic = function (name) {
        var topic = this.topics[name];
        if (topic) {
            return topic;
        }
        else {
            return this.newTopic(name);
        }
    };
    EventBusService.prototype.newTopic = function (name) {
        var event = new TopicHandler();
        this.topics[name] = event;
        return event;
    };
    //not in use atm.
    EventBusService.prototype.removeTopic = function (name) {
        this.topics[name] = null;
    };
    EventBusService = __decorate([
        core_1.Injectable(), 
        __metadata('design:paramtypes', [])
    ], EventBusService);
    return EventBusService;
}());
exports.EventBusService = EventBusService;
//contains all the subscribers on a specific string
var TopicHandler = (function () {
    function TopicHandler() {
        this.subscriptions = new Array();
    }
    TopicHandler.prototype.subscribe = function (callBackFunc) {
        this.subscriptions.push(callBackFunc);
    };
    TopicHandler.prototype.unsubscribe = function (callBackFunc) {
        var i = this.subscriptions.indexOf(callBackFunc);
        if (i > -1) {
            this.subscriptions.splice(i, 1);
        }
    };
    TopicHandler.prototype.tweet = function (args) {
        for (var _i = 0, _a = this.subscriptions; _i < _a.length; _i++) {
            var handler = _a[_i];
            handler(args);
        }
    };
    return TopicHandler;
}());
//# sourceMappingURL=event-bus.service.js.map