import { Injectable, Inject } from '@angular/core';
import { Subject } from 'rxjs/Subject'; 
import { JSONObject } from './explorer-node.component'; 

@Injectable()
export class Ng2ItemExplorerService {
    testData = {
	first: "iamfirst",
	second: {
	    "second_":"is",
	    "an":"object"
	},
	third: 2,
	fourth: {
	    nested: {
		"hi":"hihihi"
	    },
	    "woop": "floop"
	},
	I_AM_AN_ARRAY: [
	    {"oh":"shit"},
	    {"what":"up"}
	]
    };
    
    getNode: (keys: Array<string>) => Object;
    selectModel: (name: string) => void;

    private currentModelSubject = new Subject<Object>();
    currentModel = this.currentModelSubject.asObservable();
    
    private modelNamesSubject = new Subject<Array<string>>();
    modelNames = this.modelNamesSubject.asObservable();
    
    refresh: () => void;

    private loadJson: (json: Object) => void;
    root: JSONObject = new JSONObject("root", {});

    
    constructor(
	@Inject('restService') restService
    ){
	this.getNode = (keys: Array<string>) => {
	    // this is where it should fetch nodes from restservice to make it lazy
	    var data = this.testData;
	    for(var key of keys) {
		data = data[key];
	    }
	    return data;
	}

	this.selectModel = (name: string) => {
	    console.log("Selected model: " + name);
	    this.currentModelSubject.next(this.getNode([])); // top level node
	}
	
	this.refresh = () => {
	    restService.getModels().then( (data) => {
		this.modelNamesSubject.next(data);
	    });
	    this.loadJson(this.testData);
	}

	this.loadJson = (json: Object) => {
	    this.root = new JSONObject("root_node", json);
	    console.log(this.root);
	}
    }

    ngOnInit(){
	this.refresh();
    }

}

