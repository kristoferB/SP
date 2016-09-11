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

import {Injectable} from '@angular/core';

@Injectable()
export class EventBusService {
    private topics: {[name: string]: TopicHandler<any>; };

    public constructor(){
        this.topics = {};
    }

    public testToSubscribe(){
        console.log("trying out the event-bus");
        this.subscribeToTopic<string>("test", (bus=this) => {confirmed(bus)}, callback);

        function confirmed(busService:EventBusService){
            console.log("subscribtion confirmed!");
            busService.tweetToTopic<string>("test","hejhej!");
        }
        function callback(data2:string){
            console.log("callback");
            console.log(data2);
        }
    }

    public subscribeToTopic<TArgs>(name:string, confirmFunc, func: (ta:TArgs) => void){
        let topic = this.getTopic<TArgs>(name);
        topic.subscribe(func);
        confirmFunc();
    }

    public unsubscribeToTopic<TArgs>(name:string, func: (ta:TArgs) => void){
        let topic = this.getTopic(name);
        topic.unsubscribe(func);
    }

    public tweetToTopic<TArgs>(name:string, args:TArgs){
        let topic = this.getTopic(name);
        topic.tweet(args);
    }


    //privates
    private getTopic<TArgs>(name: string): TopicHandler<TArgs> {
        let topic = this.topics[name];
        if (topic) {
            return topic;
        } else {
            return this.newTopic(name);
        }
    }

    private newTopic<TArgs>(name: string) : TopicHandler<TArgs> {
        let event = new TopicHandler<TArgs>();
        this.topics[name] = event;
        return event;
    }

    //not in use atm.
    private removeTopic(name: string): void {
        this.topics[name] = null;
    }
}


//contains all the subscribers on a specific string
class TopicHandler<TArgs>{
    private subscriptions: Array<(args: TArgs) => void > = new Array<( args: TArgs) => void>();

    public subscribe(callBackFunc: ( args: TArgs) => void) {
        this.subscriptions.push(callBackFunc);
    }

    public unsubscribe(callBackFunc: ( args: TArgs) => void) {
        let i:number = this.subscriptions.indexOf(callBackFunc);
        if (i > -1) {
            this.subscriptions.splice(i, 1);
        }
    }

    public tweet(args: TArgs) {
        for (let handler of this.subscriptions){
            handler(args);
        }
    }
}
