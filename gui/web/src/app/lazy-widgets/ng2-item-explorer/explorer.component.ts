import { Component, Inject } from '@angular/core';
import { Ng2ItemExplorerService } from './explorer.service';
import { Subscription } from 'rxjs/Subscription';
import { ItemExplorerNodeComponent } from './explorer-node.component'; 
import { JsonNode } from './JsonNode';

@Component({
    selector: 'explorer',
    templateUrl: 'app/lazy-widgets/ng2-item-explorer/explorer.component.html',
    providers: [Ng2ItemExplorerService],
    directives: [ItemExplorerNodeComponent]
})
export class Ng2ItemExplorerComponent {
    refresh: () => void;
    private subscriptions: { [key:string]:Subscription; } = {};
    modelNames: Array<string> = [];
    selectModel: (name: string) => void;
    currentModel: Object = {};
    service: Ng2ItemExplorerService;

    constructor(
	ng2ItemExplorerService: Ng2ItemExplorerService
    ) {
	this.service = ng2ItemExplorerService;
	
	this.selectModel = ng2ItemExplorerService.selectModel;
	
        this.refresh = () => {
	    ng2ItemExplorerService.refresh();
	}

	this.subscriptions["modelNames"] =  ng2ItemExplorerService.modelNames.subscribe(
	    data => {
		this.modelNames = data;
	    }
	);
	this.subscriptions["currentModel"] = ng2ItemExplorerService.currentModel.subscribe(
	    data => {
		this.currentModel = data;
		console.log(this.currentModel);
	    }
	);
    }
    
    ngOnInit(){
	
    }
}

