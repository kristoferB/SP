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

    //setMode(mode: string) {
    //    this.options.mode = mode;
    //    //if (mode === 'code') { TODO translate whatever this does to ng2
    //    //    $timeout(function() {
    //    //        this.editor.editor.setOptions({maxLines: Infinity});
    //    //        this.editor.editor.on('change', function() {
    //    //            $timeout(function() {
    //    //                this.numberOfErrors = this.editor.editor.getSession().getAnnotations().length;
    //    //            }, 300);
    //    //        });
    //    //    });
    //    //}
    //}


    inSync: boolean = true;
    //unSync() {
    //    widget.storage.atLeastOneItemChanged = true;
    //}
    //showDetail: boolean = false;

    //editor: any = null;
    //transformService: string = '';

    //editorLoaded(editorInstance: any) {
    //    editor = editorInstance;
    //    editorInstance.setName('Selected items');
    //    updateSelected(itemService.selected,[]);
    //    actOnSelectionChanges();
    //    $scope.$on('itemUpdate', function() {change();});
    //}

    options: any;
    save: () => void;
    setMode: (mode: string) => void;

    itemService: any;
    eventBusService: EventBusService;

    constructor(
        @Inject('itemService') itemService,
        //@Inject('spServicesService') spServicesService,
        //@Inject('transformService') transformService
        eventBusService: EventBusService
    ) {
        this.itemService = itemService;
        this.eventBusService = eventBusService;
        eventBusService.subscribeToTopic<any>("minTopic", () => {
            console.log("I confirm");
        }, this.callback);
        
        setTimeout(() => {
            eventBusService.tweetToTopic<any>("minTopic",
                                 this.itemService.items.map((x) => x.id))
        }, 2000);

        this.options = { mode: 'tree' };

        this.save = () => {
            //itemService.saveItem(this.jec.getJson());

            //var keys = Object.keys(this.widget.storage.data);
            var json = this.jec.getJson();
            var keys = Object.keys(json);
            //var visibleJson = this.jec.getJson();
            for (var i = 0; i < keys.length; i++) {
                var key = keys[i];
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

    callback = (data: any) => {
        var json: any = {};
        for (var i: number = 0; i < data.length; i++) {
            var item: any = this.itemService.getItem(data[i]);
            json[item.name] = item;
        }
        this.jec.setJson(json);
    }

    ngOnDestroy() {
        this.eventBusService.unsubscribeToTopic("minTopic", this.callback);
    }

    //change() {
    //    if (inSync) {
    //        var keys = Object.keys(widget.storage.data);
    //        var atLeastOneItemChanged = false;
    //        for (var i = 0; i < keys.length; i++) {
    //            var key = keys[i];
    //            if (widget.storage.data.hasOwnProperty(key)) {
    //                var editorItem = widget.storage.data[key];
    //                var centralItem = itemService.getItem(editorItem.id);
    //                var equal = _.isEqual(editorItem, centralItem);
    //                widget.storage.itemChanged[editorItem.id] = !equal;
    //                if (!equal) {
    //                    atLeastOneItemChanged = true;
    //                }
    //            }
    //        }
    //        widget.storage.atLeastOneItemChanged = atLeastOneItemChanged;
    //    } else {

    //    }
    //}

    //setActiveColor(number): void {
    //    console.log('called setActiveColor');
    //}

}
