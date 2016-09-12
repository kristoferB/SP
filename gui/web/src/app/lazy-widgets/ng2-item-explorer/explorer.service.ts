import { Injectable, Inject } from '@angular/core';
import { Subject } from 'rxjs/Subject'; 
import { HierarchyNode, HierarchyRoot, Item } from '../../spTypes';

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
	@Inject('modelService') modelService
    ){	
	this.refresh = () => {
	    restService.getModels().then( (data) => {
		this.modelNamesSubject.next(data);
	    });
	}
	
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
	}

	this.activeModel = modelService.activeModel;
	this.model = itemService.items;
	this.structures = this.getRoots(this.model);
	console.log("strucutres: ");
	console.log(this.structures);
    }

    ngOnInit(){
	//this.refresh();	
    }

}
