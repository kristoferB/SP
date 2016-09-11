import { Component, Inject, Input } from '@angular/core';
import { Ng2ItemExplorerService } from './explorer.service';
import { Subscription } from 'rxjs/Subscription';


@Component({
    selector: 'explorer-node',
    templateUrl: 'app/lazy-widgets/ng2-item-explorer/explorer-node.component.html',
    directives: [ItemExplorerNodeComponent]
})

export class ItemExplorerNodeComponent {
    @Input() json: Object;
    myKey: string;
    value: Array<ItemExplorerNodeComponent>;
    type: string;
    
    constructor(myKey: string, json: Object) {
	for(let key in json){
	    this.value = new Array<ItemExplorerNodeComponent>();
	    if(Array.isArray(json[key])){
		this.type = 'array'; //  'array' in not a type in javascript (it's just an object)
		var i = 0;
		for(let thing of (json as Array<Object>)) {
		    this.value.push(new ItemExplorerNodeComponent(i.toString(), thing));
		}
		
	    } else if(typeof(json[key]) == 'object') {
		this.type = 'object';
		
	    } else { // current node is a leaf
		this.value = json[key];
		this.type = typeof(json[key]);
	    }   
	}
    }    
}

export class Node {
    type = "undefined";
    //create observable
    // id: string;
    public children = [];
    public name: string;
    public isValue = false;
    constructor(name: string){
	this.name = name;
    }
}



export class JSONObject extends Node {
    constructor(name: string, json: Object){
	super(name);
	this.type = "JSONObject";
	console.log(name + ": "+json);
	for(let key in json) {
	    let child = json[key];
	    if(Array.isArray(child)){
		this.children.push(new JSONArray(key, child));
	    } else if(typeof(child) == 'object'){ // do not move this up - typeof(Array<>) is object
		this.children.push(new JSONObject(key, child));
	    } else {
		this.children.push(new JSONValue(key, child));
	    }
	}
    }
}

export class JSONArray extends Node {
    constructor(name: string, json: Array<Object>){
	console.log(name + ": "+json);
	super(name);
	this.type = "JSONArray";
	var i = 0;
	for(let child of json) {
	    this.children.push(new JSONObject(i.toString(), child));
	    i += 1;
	}
    }
}

export class JSONValue extends Node{
    value: any;
    constructor(name: string, json: any) {
	super(name);
	this.type = "JSONValue";
	this.isValue = true;
	console.log(name + ": "+json);
	this.value = json;
    }
}



