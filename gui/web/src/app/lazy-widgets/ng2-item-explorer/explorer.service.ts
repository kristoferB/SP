import { Injectable, Inject } from '@angular/core';
import { Subject } from 'rxjs/Subject'; 
import { JsonNode } from './JsonNode';

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
	    {"an":"element"},
	    {"another":"element"}
	]
    };

    testNode: JsonNode;
    getNode: (keys: Array<string>) => Object;
    selectModel: (name: string) => void;

    private currentModelSubject = new Subject<Object>();
    currentModel = this.currentModelSubject.asObservable();
    
    private modelNamesSubject = new Subject<Array<string>>();
    modelNames = this.modelNamesSubject.asObservable();
    
    refresh: () => void;
    
    constructor(
	@Inject('restService') restService
    ){
	this.testNode = new JsonNode(this.testData);
	console.log(this.testNode);
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
	}
    }

    ngOnInit(){
	this.refresh();
    }

}
