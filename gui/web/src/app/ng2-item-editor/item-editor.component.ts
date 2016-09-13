import { Component, Input, Inject, ViewChild, OnDestroy } from '@angular/core';

import { DROPDOWN_DIRECTIVES } from 'ng2-bootstrap';
import * as _ from 'lodash';

import { JsonEditorComponent } from '../json-editor/json-editor.component';
import { EventBusService } from '../core/event-bus.service';

@Component({
    selector: 'item-editor',
    templateUrl: 'app/ng2-item-editor/item-editor.component.html',
    directives: [DROPDOWN_DIRECTIVES, JsonEditorComponent]
})

export class ItemEditorComponent implements OnDestroy {

    @Input() widget;
    @ViewChild(JsonEditorComponent) jec: JsonEditorComponent;

    // allting nonsens-satt for now
    numberOfErrors: number = 0;
    modes: string[] = ['tree', 'code'];
    editorName: string = 'Selected items';

    options: any;
    save: () => void;
    setMode: (mode: string) => void;

    itemService: any;
    eventBusService: EventBusService;

    constructor(
        @Inject('itemService') itemService,
        eventBusService: EventBusService
    ) {
        this.itemService = itemService;
        this.eventBusService = eventBusService;
        eventBusService.subscribeToTopic<any>("minTopic", () => {
        }, this.eventCallback);

        setTimeout(() => {
            eventBusService.tweetToTopic<any>("minTopic",
                this.itemService.items.map((x) => x.id))
        }, 2000);

        this.options = { mode: 'tree' };

        this.save = () => {
            var json = this.jec.getJson();
            var keys = Object.keys(json);
            for (var key of keys) {
                if (json.hasOwnProperty(key)) {
                    var editorItem = json[key];
                    var centralItem = this.itemService.getItem(editorItem.id);
                    if (!_.isEqual(editorItem, centralItem)) {
                        this.itemService.saveItem(editorItem);
                    }
                }
            }
        }

        this.setMode = (mode: string) => {
            this.options.mode = mode;
            this.jec.setMode(mode);
        }
    }

    eventCallback = (data: any) => {
        var json: any = {};
        for (var j of data) {
            console.log(j);
            let item = this.itemService.getItem(j);
            var keyName = item.name; var count = 0;
            while (!_.isUndefined(_.find(Object.keys(json), (k) => { return k == keyName; }))) {
                keyName = item.name + ' (' + (++count) + ')';
            }
            json[keyName] = item;
        }
        this.jec.setJson(json);
    }

    ngOnDestroy() {
        this.eventBusService.unsubscribeToTopic("minTopic", this.eventCallback);
    }
}
