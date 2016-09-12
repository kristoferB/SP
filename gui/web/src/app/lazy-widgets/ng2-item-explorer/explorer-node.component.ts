import { Component, Inject, Input } from '@angular/core';
import { Ng2ItemExplorerService } from './explorer.service';
import { Subscription } from 'rxjs/Subscription';
import { HierarchyNode } from '../../spTypes';
import {EventBusService} from "../../core/event-bus.service";


@Component({
    selector: 'explorer-node',
    templateUrl: 'app/lazy-widgets/ng2-item-explorer/explorer-node.component.html',
    directives: [ItemExplorerNodeComponent]
})

export class ItemExplorerNodeComponent {
    @Input() node: HierarchyNode;

    name: string = "";
    expanded: boolean = false;
    getName: (id: string) => string;
    sendSelected: () => void;
    
    constructor(
	@Inject('itemService') itemService,
    evBus: EventBusService

    ){
	this.getName = (id: string) => {
	    //TODO null check
	    return itemService.getItem(id).name
	}

	this.sendSelected = () => {
	    console.log("VI klickar");
	    console.log(this.node);
	    console.log(this.getName(this.node.item));
        evBus.tweetToTopic<any>("minTopic", [this.node.item]);
    }

    }

    ngOnInit(){
	this.name = this.getName(this.node.item);
    }
}





