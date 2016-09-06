/**
 * Created by oskar on 06/09/16.
 */

//not in use atm.
import {Component} from '@angular/core';


export class EventBusService{
    //TODO:
    //START
    //STOP
}

export class EventBus {
    private _topics: { [name: string]: TopicHandler<any>; } = {};

    public subscribeToTopic<TArgs>(name:string, confirmFunc, func: (ta:TArgs) => void){
        let topic = this.getTopic(name);
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
        let topic = this._topics[name];
        if (topic) {
            return topic;
        } else {
            return this.newTopic(name);
        }
    }

    private newTopic<TArgs>(name: string) : TopicHandler<TArgs> {
        let event = new TopicHandler<TArgs>();
        this._topics[name] = event;
        return event;
    }

    //not in use atm.
    private removeTopic(name: string): void {
        this._topics[name] = null;
    }
}


interface callBackFunc<TSender,TArgs>{ sender:TSender, args:TArgs }
//TODO: replace fn:(sender,args) in TopicHandler with this


class TopicHandler<TArgs>{
    private _subscriptions: Array<(args: TArgs) => void > = new Array<( args: TArgs) => void>();

    public subscribe(callBackFunc: ( args: TArgs) => void) {
        this._subscriptions.push(callBackFunc);
    }

    public unsubscribe(callBackFunc: ( args: TArgs) => void) {
        let i:number = this._subscriptions.indexOf(callBackFunc);
        if (i > -1) {
            this._subscriptions.splice(i, 1);
        }
    }

    public tweet(args: TArgs) {
        for (let handler of this._subscriptions){
            handler(args);
        }
    }
}
