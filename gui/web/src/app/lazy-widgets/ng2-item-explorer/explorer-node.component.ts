import { Component, Inject, Input } from '@angular/core';
import { Ng2ItemExplorerService } from './explorer.service';
import { Subscription } from 'rxjs/Subscription';
import { HierarchyNode } from '../../spTypes';

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
    
    constructor(
	@Inject('itemService') itemService
    ){
	this.getName = (id: string) => {
	    //TODO null check
	    return itemService.getItem(id).name
	}
    }
    ngOnInit(){
	this.name = this.getName(this.node.item);
    }
}





