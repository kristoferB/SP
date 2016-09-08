import { Component, Inject } from '@angular/core';

@Component({
    selector: 'explorer',
    templateUrl: 'app/lazy-widgets/ng2-item-explorer/explorer.component.html'
})
export class Ng2ItemExplorerComponent {
    model: any = "wadup";
    refresh: () => void;
    constructor(
        @Inject('modelService') modelService
    ) {
        console.log(modelService);
        this.refresh = () => {
            console.log("clikced ");
            this.model = modelService.models[0];
        }
    }
}
