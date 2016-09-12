import { Component, Inject, Input } from '@angular/core';
import { Ng2ItemExplorerService } from './explorer.service';
import { Subscription } from 'rxjs/Subscription';
import { JsonNode } from './JsonNode';

@Component({
    selector: 'explorer-node',
    templateUrl: 'app/lazy-widgets/ng2-item-explorer/explorer-node.component.html',
    directives: [ItemExplorerNodeComponent]
})

export class ItemExplorerNodeComponent {
    @Input() node: JsonNode;
    constructor(){}
}




