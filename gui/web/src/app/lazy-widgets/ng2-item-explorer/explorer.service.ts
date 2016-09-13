import { Injectable, Inject, OnInit } from '@angular/core';
import { Subject } from 'rxjs/Subject'; 
import { HierarchyNode, HierarchyRoot, Item } from '../../spTypes';
import {EventBusService} from "../../core/event-bus.service";

@Injectable()
export class Ng2ItemExplorerService {
    selectModel: (name: string) => void;

    private currentModelSubject = new Subject<Object>();
    currentModel = this.currentModelSubject.asObservable();
    
    private modelNamesSubject = new Subject<Array<string>>();
    modelNames = this.modelNamesSubject.asObservable();
    
    refresh: () => void;
    activeModel: string = "";
    model:  Array<Object>;
    getRoots: (model: Array<Object>) => Array<HierarchyRoot>; 
    structures: Array<HierarchyRoot>;
    
    constructor(
	@Inject('restService') restService,
	@Inject('itemService') itemService,
	@Inject('modelService') modelService,
	evBus: EventBusService
    ){	
	this.refresh = () => {
	    this.activeModel = modelService.activeModel;
	    this.model = itemService.items;
	    this.structures = this.getRoots(this.model);
	    console.log("strucutres: ");
	    console.log(this.structures);

	    let idList = this.structures.map(x => x.id)

	    evBus.tweetToTopic<any>("minTopic", idList);
	};

	this.getRoots = (model: Array<HierarchyRoot>) => {
	    console.log('filtering model: ');
	    console.log(model);
	    let roots = new Array<HierarchyRoot>();
	    for(let element of model){
		if(element['isa'] == 'HierarchyRoot'){
		    roots.push(element);
		}
	    }
	    return roots;
	};

    }
}
