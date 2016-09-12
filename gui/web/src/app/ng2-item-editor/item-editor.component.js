"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
var core_1 = require('@angular/core');
var ng2_bootstrap_1 = require('ng2-bootstrap');
var json_editor_component_1 = require('../json-editor/json-editor.component');
var ItemEditorComponent = (function () {
    function ItemEditorComponent(itemService) {
        var _this = this;
        // allting nonsens-satt for now
        this.numberOfErrors = 0;
        this.modes = ['tree', 'code'];
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
        this.inSync = true;
        this.options = { mode: 'tree' };
        this.save = function () {
            itemService.saveItem(_this.jec.getJson());
            //itemService.saveItem('{"isa": "Operation","name": "24u","conditions": [],"attributes": {},"id": "e53"}')
            //if (this.inSync) {
            //    var keys = Object.keys(this.widget.storage.data);
            //    for (var i = 0; i < keys.length; i++) {
            //        var key = keys[i];
            //        if (this.widget.storage.data.hasOwnProperty(key)) {
            //            // TODO denna variabel sparas av item-explorer
            //            // TODO hur lösa?
            //            var editorItem = this.widget.storage.data[key];
            //            var centralItem = itemService.getItem(editorItem.id);
            //            if (!_.isEqual(editorItem, centralItem)) {
            //                //angular.extend(centralItem, editorItem);
            //                itemService.saveItem(editorItem);
            //            }
            //        }
            //    }
            //    this.widget.storage.atLeastOneItemChanged = false;
            //} else {
            //    console.log("call service")
            //    spServicesService.callService(spServicesService.getService(transformService), {data: this.widget.storage.data}, response)
            //}
            //function response(event){
            //    this.widget.storage.data = event;
            //}
        };
        this.setMode = function (mode) {
            _this.options.mode = mode;
            _this.jec.setMode(mode);
        };
    }
    __decorate([
        core_1.Input(), 
        __metadata('design:type', Object)
    ], ItemEditorComponent.prototype, "widget", void 0);
    __decorate([
        core_1.ViewChild(json_editor_component_1.JsonEditorComponent), 
        __metadata('design:type', json_editor_component_1.JsonEditorComponent)
    ], ItemEditorComponent.prototype, "jec", void 0);
    ItemEditorComponent = __decorate([
        core_1.Component({
            selector: 'item-editor',
            templateUrl: 'app/ng2-item-editor/item-editor.component.html',
            directives: [ng2_bootstrap_1.DROPDOWN_DIRECTIVES, json_editor_component_1.JsonEditorComponent]
        }),
        __param(0, core_1.Inject('itemService')), 
        __metadata('design:paramtypes', [Object])
    ], ItemEditorComponent);
    return ItemEditorComponent;
}());
exports.ItemEditorComponent = ItemEditorComponent;
//# sourceMappingURL=item-editor.component.js.map